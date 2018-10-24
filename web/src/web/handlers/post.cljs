(ns web.handlers.post
  (:require [share.query :as query]
            [share.util :as util]
            [share.config :as config]
            [share.dicts :refer [t]]
            [goog.object :as gobj]
            [web.scroll :as scroll]
            [web.md5 :as md5]
            [clojure.string :as str]
            [appkit.macros :refer [oget oset!]]))

(defn- add
  [state k id]
  (update state k
          (fn [v] (if id
                    (set (conj v id))
                    v))))

(defn- delete
  [state k id]
  (update state k
          (fn [v] (set (remove #(= id %) v)))))

(defn post-params
  [state params specific-user-id]
  (let [current-path (get-in state [:router :handler])
        post-filter (cond
                      (= current-path :home)
                      :hot

                      (contains? #{:book} current-path)
                      :latest-reply

                      (contains? #{:user} current-path)
                      :latest

                      (= current-path :votes)
                      :voted

                      :else
                      (or
                       (:filter params)
                       (get-in state [:post :filter] :hot)))

        [params path] (cond
                        specific-user-id
                        [{:user_id specific-user-id}
                         (:merge-path params)]

                        (:book_id params)
                        [{:book_id (:book_id params)}
                         (:merge-path params)]

                        ;; home
                        :else
                        [nil (:merge-path params)])]
    [current-path post-filter params path]))

(defn set-post-form-data
  ([state v]
   (set-post-form-data state v nil))
  ([state v {:keys [completed?]
             :or {completed? false}}]
   (let [body (:body v)
         form-data (get-in state [:post :form-data])
         form-data (merge form-data v)
         current-path (get-in state [:router :handler])
         body (:body form-data)
         current-user (get-in state [:user :current])
         save-draft? (and current-user
                          (= current-path :new-post)
                          (or (:title form-data)
                              body)
                          (not (get-in state [:post :saving?])))]
     (cond->
       {:state (cond->
                 (assoc-in state [:post :form-data] form-data)
                 save-draft?
                 (assoc-in [:post :saving?] true)
                 (not (str/blank? (:title v)))
                 (assoc-in [:post :post-title-exists?] false))}

       (and body
            (not completed?))
       (assoc :dispatch [:citrus/auto-complete body])

       save-draft?
       (assoc :dispatch [:post/new-draft form-data])))))

(def handlers
  {:citrus/load-more-posts
   (fn [state params]
     (let [last-post (if (map? (:last params))
                       (:last params)
                       @(:last params))
           specific-user-id (:user_id params)
           [current-path post-filter params path]  (post-params state params specific-user-id)
           params (->> {:filter post-filter
                        :cursor (case post-filter
                                  :hot
                                  {:after (:rank last-post)
                                   :where [:and
                                           [:or
                                            [:< :rank (:rank last-post)]
                                            [:and
                                             [:= :rank (:rank last-post)]
                                             [:< :flake_id (:flake_id last-post)]]]]}

                                  :latest
                                  {:after (:flake_id last-post)}

                                  :latest-reply
                                  (if-let [last-reply-at (:last_reply_at last-post)]
                                    {:after last-reply-at
                                     :where [[:< :last-reply-at last-reply-at]]}
                                    {:after (:flake_id last-post)
                                     :where [[:< :flake_id (:flake_id last-post)]]})

                                  :voted
                                  {:after (:flake_id last-post)})}
                       (merge params))]
       {:state (-> state
                   (assoc-in [:post :filter] post-filter))
        :dispatch [:query/send current-path
                   {:q {:posts {:fields query/post-fields}}
                    :args {:posts (dissoc params :merge-path)}
                    :merge {:posts path}}
                   true]}))


   :post/update
   (fn [state data]
     {:state {:loading? true}
      :http {:params [:post/update data]
             :on-load :citrus/update-ready
             :on-error :post/update-failed}})

   :post/update-failed
   (fn [state result]
     {:state (->
              (cond
                (and (= (:status result) 400)
                     (= (get-in result [:body :message]) ":post-permalink-exists"))
                {:post-permalink-exists? true}

                (and (= (:status result) 400)
                     (= (get-in result [:body :message]) ":post-title-exists"))
                {:post-title-exists? true}

                (and (= (:status result) 400)
                     (= (get-in result [:body :message]) ":post-link-exists"))
                {:post-link-exists? true}

                :else
                state)
              (assoc :loading? false))})


   :citrus/update-ready
   (fn [state result]
     (prn (:permalink result))
     (prn result)
     (util/set-href! (str config/website "/" (:permalink result)))
     {:state state})

   :post/delete
   (fn [state post]
     {:state {:loading? true}
      :http {:params [:post/delete {:id (:id post)}]
             :on-load [:citrus/post-delete-ready post]}})

   :citrus/post-delete-ready
   (fn [state post result]
     (when-let [current-user (get-in state [:user :current])]
       (let [handler (get-in state [:router :handler])
             path (case handler
                    :drafts
                    "drafts"
                    (str "@" (or (get-in post [:user :screen_name])
                                 (:screen_name current-user))))]
         (util/set-href! (str config/website "/" path))
         ))
     {:state state})

   :post/top
   (fn [state id]
     {:state (-> state
                 (add :toped id))
      :http {:params [:post/top {:id id}]
             :on-load [:post/top-ready id]}})

   :post/top-ready
   (fn [state id result]
     {:state state})

   :post/untop
   (fn [state id]
     {:state (-> state
                 (delete :toped id))
      :http {:params [:post/untop {:id id}]
             :on-load [:post/untop-ready id]}})

   :post/untop-ready
   (fn [state id result]
     {:state state})

   :post/open-delete-dialog?
   (fn [state post]
     {:state {:delete-dialog? true
              :delete-post post}}
     )

   :post/close-delete-dialog?
   (fn [state]
     {:state {:delete-dialog? false
              :delete-post nil}})

   :post/set-filter
   (fn [state filter]
     {:state {:filter filter}})

   :citrus/toggle-preview
   (fn [state]
     (let [post? (contains?
                  #{:post-edit :new-post}
                  (get-in state [:router :handler]))]
       {:state (update-in state [(if post? :post :comment) :form-data :preview?] not)}))

   :citrus/set-post-form-data
   set-post-form-data

   :post/new-link
   (fn [state form-data]
     {:state {:loading? true}
      :http {:params [:post/new-link @form-data]
             :on-load :citrus/update-ready
             :on-error :post/update-failed}})

   ;; server will redirect to post-edit
   :post/new-draft
   (fn [state form-data]
     {:state {:saving? true}
      :http {:params [:post/new (-> (select-keys form-data [:title :body :body_format])
                                    (assoc :is_draft true))]
             :on-load :post/new-draft-ready
             :on-error :post/new-draft-failed}})

   ;; TODO: add to drafts
   :post/new-draft-ready
   (fn [state result]
     {:state {:saving? false
              :current result}
      :redirect {:handler :post-edit
                 :route-params {:post-id (str (:id result))}}})

   :post/new-draft-failed
   (fn [state result]
     {:state {:saving? false}})

   ;; save every 5 seconds, new-post or post-edit
   :post/save
   (fn [state]
     (let [{:keys [images] :as form-data} (:form-data state)]
       (if (seq (select-keys form-data [:title :body]))
         (let [first-image (and (= 1 (count images))
                                (:url (second (first images))))
               remove-nil? (partial util/remove-v-nil? :v)]
           (if-let [current (:current state)]
             (let [data (assoc (select-keys form-data
                                            [:title :body :body_format])
                               :id (:id current))
                   data (if (and (nil? (:cover current))
                                 first-image)
                          (assoc data :cover first-image)
                          data)]

               (if (or (and
                        (:title form-data)
                        (not= (:title current) (:title form-data)))
                       (and
                        (:body form-data)
                        (not= (:body current) (:body form-data))))
                 {:state {:saving? true}
                  :http {:params [:post/update (dissoc data
                                                       :title-validated?)]
                         :on-load :post/save-ready
                         :on-error :post/save-failed}}
                 {:state state}))
             {:state state}))
         {:state state})))

   :post/save-ready
   (fn [state result]
     {:state {:saving? false
              :current (merge (:current state) result)}})

   :post/save-failed
   (fn [state result]
     {:state (merge
              {:saving? false}
              (cond
                (and (= (:status result) 400)
                     (= (get-in result [:body :message]) ":post-title-exists"))
                {:post-title-exists? true}

                :else
                (assoc state
                       :clear-interval? true)))})

   :post/reset-form-data
   (fn [state]
     {:state {:form-data nil}})

   :citrus/set-default-body-format
   (fn [state]
     (let [format (or (:latest-body-format state)
                      :markdown)]
       {:state (assoc-in state [:post :form-data]
                        {:body_format format})}))

   :citrus/save-latest-body-format
   (fn [state body-format]
     (if (= (get-in state [:router :handler])
            :post-edit)
       {:state {:latest-body-format body-format}
        :local-storage {:action :set
                        :key :latest-body-format
                        :value body-format}}
       {:state state}))

   :post/read
   (fn [state post]
     {:state {:read-list (assoc (:read-list state) (:id post) true)}
      :http {:params [:post/read {:id (:id post)}]
             :on-load :post/read-success
             :on-error :post/read-failed}})

   :post/read-success
   (fn [state result]
     {:state state})

   :post/read-failed
   (fn [state error]
     (prn "mark as read failed, should retry several times.")
     {:state state})

   :post/opengraph-query
   (fn [state url ok-handler]
     (prn url)
     {:state {:opengraph-processing? true}
      :http {:params [:opengraph/query {:link url}]
             :on-load [:post/opengraph-query-success url ok-handler]
             :on-error [:post/opengraph-query-failed url]}})

   :post/opengraph-query-success
   (fn [state url ok-handler result]
     (prn "opengraph query result: " result)
     (ok-handler result)
     {:state {:opengraph-processing? false
              :opengraph (assoc (:opengraph state) url result)}})

   :post/opengraph-query-failed
   (fn [state url error]
     (prn "opengraph query error: " error)
     {:state {:opengraph-processing? false
              :opengraph-errors (assoc (:opengraph-errors state)
                                       url
                                       error)}})

   })
