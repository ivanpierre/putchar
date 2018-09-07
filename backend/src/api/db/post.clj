(ns api.db.post
  (:refer-clojure :exclude [get update])
  (:require [clojure.java.jdbc :as j]
            [api.db.util :as util]
            [api.db.cache :as cache]
            [api.db.user :as u]
            [api.db.group :as group]
            [api.db.top :as top]
            [api.db.search :as search]
            [api.db.choice :as choice]
            [clojure.string :as str]
            [api.util :as au]
            [taoensso.carmine :as car]
            [honeysql.core :as sql]
            [honeysql.helpers :as sql-helpers]
            [api.db.star :as star]
            [pinyin4clj.core :refer [ascii-pinyin]]
            [api.config :refer [config]]
            [share.config :as sc]
            [share.asciidoc :as ascii]
            [api.pg.types]
            [clj-time
             [coerce :as tc]
             [core :as t]
             [format :as tf]]
            [share.util :as su]
            [bidi.bidi :as bidi]
            [api.services.slack :as slack]
            [clojure.set :as set]
            [api.db.moderation-log :as mlog]
            [share.content :as content]))

(defonce ^:private table :posts)
(defonce ^:private fields [:*])
(def ^:private base-map {:select [:id :flake_id :user_id :user_screen_name
                                  :group_id :group_name
                                  :title :tops
                                  :rank :comments_count :permalink
                                  :created_at :updated_at :last_reply_at :last_reply_by :last_reply_idx :last_reply_idx :frequent_posters
                                  :canonical_url
                                  :lang
                                  :body :body_format :tags
                                  :choices :cover :video]
                         :from [table]})

;; user-screen-name => (map of tag * post-count)
(def tags-k "post-user-tags")

(defn with-group
  [post]
  (some-> post
          (util/with :group_id (fn [_]
                                 {:id (:group_id post)
                                  :name (:group_name post)}))))

(defn normalize
  [db post]
  (some-> post
          (clojure.core/update :choices su/read-string)
          (util/with :user_id #(u/get db % [:id :screen_name :name :bio :website]))
          (util/with :group_id (fn [_]
                                 (let [group (group/get db (:group_id post) [:stars :admins :purpose :type :created_at :related_groups])]
                                   (merge
                                    group
                                    {:id (:group_id post)
                                     :name (:group_name post)}))))))

(defn with-user-group
  [post]
  (some-> post
          (util/with :user_id (fn [_]
                                {:id (:user_id post)
                                 :screen_name (:user_screen_name post)}))
          (with-group)
          (dissoc :user_screen_name :group_name)))

(defn get
  ([db id-or-permalink]
   (get db id-or-permalink [:*]))
  ([db id-or-permalink fields]
   (when id-or-permalink
     (some->> (util/get db {:select fields
                           :from [table]}
                       (if (string? id-or-permalink)
                         {:permalink id-or-permalink}
                         id-or-permalink))
             (normalize db)))))

(defn get-permalink-by-id
  [db id]
  (util/select-one-field db table id :permalink))

(defn permalink [screen-name title]
  (->>
   (-> title
       (str/trim)
       (str/replace #"[「」？…,=+>%<./\_?!:;()\[\]{}@#$%^&*'：，“”‘’ `\"\。《》【】|]" "-")
       (str/lower-case))
   (ascii-pinyin)
   (take 64)
   (apply str)
   (#(str/replace % #"-+" "-"))
   (bidi.bidi/url-encode)
   (str "@" screen-name "/")))

(defn choices->text
  [choices]
  (if (seq choices)
    (pr-str choices)))

(defn safe-trim
  [x]
  (if x
    (str/trim x)))

(defn ->tags
  [tags]
  (some->> tags
           (su/split-tags)
           (distinct)
           (map su/tag-encode)
           (take 3)))

(defn update-tags
  [screen-name add-tags remove-tags]
  (let [tags-cache (cache/wcar*
                    (car/hget tags-k screen-name))
        new-tags (loop [tags add-tags tags-cache tags-cache]
                   (if (seq tags)
                     (recur (rest tags)
                            (clojure.core/update tags-cache (first tags) (fn [v] (if v (inc v) 1))))
                     tags-cache))
        new-tags (loop [tags remove-tags new-tags new-tags]
                   (if (seq tags)
                     (recur (rest tags)
                            (clojure.core/update new-tags (first tags) (fn [v] (if (and v (>= v 1)) (dec v) 0))))
                     new-tags))]
    (cache/wcar*
     (car/hset tags-k screen-name new-tags))))

(defn create
  [db data]
  (let [m (-> data
              (clojure.core/update :body safe-trim)
              (clojure.core/update :title safe-trim)
              (clojure.core/update :title su/capitalize-first-char)
              (clojure.core/update :choices choices->text))
        tags (->tags (:tags m))
        m (assoc m :tags tags)
        screen-name (or
                     (:screen_name data)
                     (:screen_name (u/get db (:user_id m) [:screen_name])))
        result (util/create db table (assoc m :user_screen_name screen-name) :flake? true)]
    (when (seq tags)
      (update-tags screen-name tags #{}))
    result))

(defn- merge-choices
  [new-choices db id ]
  (let [{:keys [choices] :as old-post} (get db id)]
    (if (seq choices)
      (let [choices (su/normalize choices)]
        (->>
         (for [choice new-choices]
           (assoc choice
                  :votes (get-in choices [(:id choice) :votes])))
         (into [])))
      new-choices)))

(defn update
  [db id m]
  (when-let [post (get db id)]
    (let [old-tags (:tags post)
          m (dissoc m :flake_id)]
      (when (seq m)
        (let [m (cond-> m
                  (:body m)
                  (clojure.core/update :body safe-trim)

                  (:title m)
                  (clojure.core/update :title (comp su/capitalize-first-char safe-trim)))
              m (if (and (:choices m) (coll? (:choices m)))
                  (assoc m :choices
                         (-> (:choices m)
                             (merge-choices db id)
                             (choices->text)))
                  m)
              tags (->tags (:tags m))
              m (if tags
                  (assoc m :tags tags)
                  m)]
          (util/update db table id (assoc m
                                          :updated_at (util/sql-now)))
          (if (seq tags)
            (let [s1 (set tags)
                  s2 (if (seq old-tags)
                       (set old-tags)
                       #{})
                  add-tags (set/difference s1 s2)
                  remove-tags (set/difference s2 s1)]
              (update-tags (:user_screen_name post)
                           add-tags remove-tags)))
          (get db id))))))

(defn delete
  ([db id-or-permalink]
   (delete db id-or-permalink nil nil))
  ([db id-or-permalink moderator reason]
   (when-let [post (get db id-or-permalink)]
     (let [{:keys [id flake_id group_id tags user_screen_name]} post]
       (util/delete db table id)
       (search/delete-post id)

       (when moderator
         (mlog/create db {:moderator moderator
                          :post_permalink (:permalink post)
                          :type "Post Delete"
                          :reason reason}))

       ;; updated tags
       (when (seq tags)
         (update-tags user_screen_name #{} (set tags)))))))

(defn brief-body
  [posts]
  (map
    (fn [{:keys [body] :as post}]
      (assoc post :body
             (apply str (take 280 body))))
    posts))

(defn flatten-frequent-posters
  [posts]
  (map (fn [{:keys [frequent_posters] :as post}]
         (let [posters (if frequent_posters
                         (->> frequent_posters
                              (read-string)
                              (sort-by (fn [x] (second x)) #(< %2 %1))
                              (take 5)
                              (keys))
                         frequent_posters)]
           (assoc post :frequent_posters
                  posters)))
    posts))

(defn get-posts
  [db where cursor]
  (->> (-> (assoc base-map :where where)
           (util/wrap-cursor cursor))
       (util/query db)
       (map with-user-group)
       (flatten-frequent-posters)))

(defn get-non-tech
  ([db cursor]
   (get-non-tech db [:and
                     [:= :is_draft false]
                     [:= :non_tech true]] cursor))
  ([db where cursor]
   (get-posts db where cursor)))

(def post-conditions
  [:and
   [:= :is_draft false]
   [:= :non_tech false]])

(defn get-new
  ([db cursor]
   (get-new db post-conditions cursor))
  ([db where cursor]
   (get-posts db where cursor)))

(defn get-hot
  ([db cursor]
   (get-hot db post-conditions cursor))
  ([db where cursor]
   (get-posts db where (merge cursor
                              {:order-key :rank
                               :order :desc}))))

(defn get-tag
  ([db tag cursor]
   (get-tag db
            tag
            [:and
             [:any (su/encode tag) "tags"]]
            cursor))
  ([db tag where cursor]
   (get-posts db where cursor)))

(defn get-user-tag
  ([db user_screen_name tag cursor]
   (get-tag db
            tag
            [:and
             [:= :user_screen_name user_screen_name]
             [:any tag "tags"]]
            cursor))
  ([db user_screen_name tag where cursor]
   (get-posts db where cursor)))

(defn get-user-tags
  [screen-name]
  (some->>
   (some->
    (cache/wcar*
     (car/hget tags-k screen-name))
    (su/keywordize))
   (filter (fn [[_ v]]
             (> v 0)))
   (sort (fn [x1 x2]
           (>= (second x1) (second x2))))))

(defn get-latest-reply
  ([db cursor]
   (get-latest-reply db post-conditions cursor))
  ([db where cursor]
   (let [order-key :last_reply_at]
     (get-posts db where (merge (dissoc cursor :after)
                                {:order-key order-key
                                 :order :desc})))))

;; hot posts selected from user's joined groups
(defn get-user-feed
  [db user-id cursor]
  (when-let [stared-groups (util/select-one-field db :users user-id :stared_groups)]
    (let [data (-> base-map
                   (assoc :where [:and
                                  [:in :group_id stared-groups]])
                   (util/wrap-cursor (assoc cursor
                                            :order-key :rank)))]
      (-> (util/query db data)
          (util/with :user_id #(u/get db % [:id :screen_name]))
          (util/with :group_id #(group/get db % [:name]))))))

(defn get-toped
  ([db user-id cursor]
   (get-toped db user-id post-conditions cursor))
  ([db user-id where cursor]
   (let [key (cache/redis-key "users" user-id "toped_posts")
         after (clojure.core/get cursor :after)
         cursor (if after
                  (assoc cursor :after (util/select-one-field db :tops {:user_id user-id
                                                                        :post_id after} :flake_id))
                  cursor)
         ids (cache/cursor key cursor)]
     (->> (util/get-by-ids db table ids {:where where})
          (map with-user-group)
          (flatten-frequent-posters)))))

(defn get-bookmarked
  ([db user-id cursor]
   (get-bookmarked db user-id post-conditions cursor))
  ([db user-id where cursor]
   (let [key (cache/redis-key "users" user-id "bookmarked_posts")
         after (clojure.core/get cursor :after)
         cursor (if after
                  (assoc cursor :after (util/select-one-field db :bookmarks {:user_id user-id
                                                                        :post_id after} :flake_id))
                  cursor)
         ids (cache/cursor key cursor)]
     (->> (util/get-by-ids db table ids {:where where})
          (map with-user-group)
          (flatten-frequent-posters)))))

(defn get-top
  ([db cursor]
   (get-top db post-conditions cursor))
  ([db where cursor]
   (-> base-map
       (assoc :where where)
       (util/wrap-cursor (merge cursor
                                {:order-key :tops
                                 :order :desc})))))

(defn inc-comments-count
  [db id]
  (j/execute! db (sql/format {:update table
                              :where [:= :id id]
                              :inc :comments_count})))

(defn dec-comments-count
  [db id]
  (when-let [c (get db id [:comments_count])]
    (when (> (:comments_count c) 0)
      (j/execute! db (sql/format {:update table
                                  :where [:= :id id]
                                  :dec :comments_count})))))

(defn top [db uid id]
  (when-let [top-result (top/top db {:user_id uid
                                     :post_id id})]
    (when-let [{:keys [tops created_at] :as post} (get db id [:tops :created_at])]
      (let [new-tops (inc tops)
            data {:tops new-tops
                  :rank (au/ranking new-tops created_at)}]
        (update db id data)
        (u/inc-karma db id)
        data))))

(defn untop [db uid id]
  (when-let [{:keys [tops created_at] :as post} (get db id [:tops :created_at])]
    (let [new-tops (if (>= tops 1) (dec tops) tops)
          data {:tops new-tops
                :rank (au/ranking new-tops created_at)}]
      (update db id data)
      (top/untop db {:user_id uid
                     :post_id id})
      (u/dec-karma db id)
      data)))

(defn view [conn uid id]
  (util/execute! conn
                 {:update table
                  :where [:= :id id]
                  :inc :views}))

(defn star
  [db post-id user-id]
  (when-let [user (u/get db user-id)]
    (let [m {:user_id user-id
             :screen_name (:screen_name user)
             :object_type "post"
             :object_id post-id}]
      (when-not (util/exists? db :stars m)
        (let [result (star/star db m)]
          (cache/wcar*
           (car/zadd (cache/redis-key "users" user-id "stared_posts") (:flake_id result) post-id))
          result)))))

(defn unstar
  [db post-id user-id]
  (let [m {:user_id user-id
           :object_type "post"
           :object_id post-id}]
    (when (util/exists? db :stars m)
      (star/unstar db m)
      (cache/wcar*
       (car/zrem (cache/redis-key "users" user-id "stared_posts") post-id)))))

(defn- group-post-conditions
  [id]
  [:and
   [:= :group_id id]
   [:= :is_draft false]])

(defn get-group-new
  [db id cursor]
  (get-new db (group-post-conditions id) cursor))

(defn get-group-hot
  [db id cursor]
  (get-hot db (group-post-conditions id) cursor))

(defn get-group-top
  [db id cursor]
  (get-top db (group-post-conditions id) cursor))

(defn get-group-latest-reply
  [db id cursor]
  (get-latest-reply db (group-post-conditions id) cursor))

(defn- user-post-conditions
  [id]
  [:and
   [:= :user_id id]
   [:= :is_draft false]])

(defn get-drafts
  [db uid cursor]
  (-> {:select [:id :flake_id :title :created_at :cover :user_id]
       :from [table]}
      (assoc :where [:and
                     [:= :is_draft true]
                     [:= :user_id uid]])
      (util/wrap-cursor cursor)
      (->> (util/query db))))

(defn get-user-new
  ([db id cursor]
   (get-user-new db id (user-post-conditions id) cursor))
  ([db id post-conditions cursor]
   (get-new db post-conditions cursor)))

(defn search
  [db q & {:keys [limit where]
           :or {limit 5
                where post-conditions}}]
  (let [result (when-not (str/blank? (:post_title q))
                 (let [result (search/search q :limit limit)]
                   (when (seq result)
                     (let [ids (->> (filter :post_id result)
                                    (mapv (comp au/->uuid :post_id)))
                           results (util/get-by-ids db :posts ids {:where where
                                                                   :order? false})
                           results (-> (util/get-by-ids db :posts ids {:where where
                                                                       :order? false})
                                       (util/with :user_id #(u/get db % [:id :screen_name]))
                                       (util/with :group_id #(group/get db % [:name])))]
                       (->>
                        (for [id ids]
                          (filter #(= (:id %) id) results))
                        (flatten)
                        (remove nil?))))))]
    (if (seq result)
      result
      [])))

(defn recalculate-rank
  [db]
  (let [posts (j/query db ["select id, tops, created_at from posts  where is_draft is not true order by created_at asc"])]
    (doseq [{:keys [id tops created_at]} posts]
      (let [rank (au/ranking tops created_at)]
        (update db id {:rank rank})))))

(defn ->rss
  [posts]
  (let [rfc822-format (tf/formatters :rfc822)]
    (for [{:keys [title body body_format permalink user group tags created_at body_format]} posts]
      {:title title
       :description (format "<![CDATA[ %s ]]>" (content/render body body_format))
       :link (str (:website-uri config) "/" permalink)
       :category (str/join ", " (cons (su/original-name (:name group))
                                      (map su/tag-decode tags)))
       :pubDate (tf/unparse rfc822-format (tc/to-date-time created_at))})))

(defn poll-choose
  [db {:keys [post_id choice_id] :as m}]
  (when-not (choice/exists? db m)
    (choice/create db m)
    ;; inc votes
    (let [{:keys [choices]} (get db post_id)]
      (update db post_id {:choices (choices->text
                                    (mapv (fn [x]
                                            (if (= choice_id (:id x))
                                              (clojure.core/update x :votes (fnil inc 0))
                                              x)) choices))}))))
