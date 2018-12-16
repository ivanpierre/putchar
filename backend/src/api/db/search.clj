(ns api.db.search
  (:require [clucie.core :as lucene]
            [clucie.analysis :as lucene-ana]
            [clucie.store :as lucene-store]
            [api.config :as config]
            [share.util :as util])
  (:import
   (org.apache.lucene.index Term IndexWriter IndexWriterConfig)
   (org.apache.lucene.search TermQuery
                             BooleanQuery BooleanClause PrefixQuery
                             BooleanClause$Occur FuzzyQuery)
   (org.apache.lucene.queryparser.classic QueryParser)))


(defonce analyzer (lucene-ana/standard-analyzer))
(defonce index-store (atom nil))

(defn init! []
  (if (nil? @index-store)
    (reset! index-store
            (lucene-store/disk-store (get-in config/config [:search :index-path])))))

(defn create-idx-writer-config []
  (let [iwc (IndexWriterConfig. analyzer)]
    (.setRAMBufferSizeMB iwc 256.0)
    iwc))

(defn delete-all
  []
  (let [dir @index-store
        iwc (create-idx-writer-config)
        writer (IndexWriter. dir iwc)]
    (doto writer
      (.deleteAll)
      (.commit)
      (.close))))

(defn add-user [user]
  (let [user {:screen_name (:screen_name user)}]
    (lucene/add! @index-store
                 [user]
                 [:screen_name]
                 analyzer)))

(defn delete-user [screen-name]
  (lucene/delete! @index-store
                  :screen_name
                  screen-name
                  analyzer))

(defn add-post [post]
  (when (not (:is_draft post))
    (let [post {:post_id (:id post)
                :post_title (:title post)}]
      (lucene/add! @index-store
                   [post]
                   [:post_id :post_title]
                   analyzer))))

(defn delete-post [id]
  (lucene/delete! @index-store
                  :post_id
                  id
                  analyzer))

(defn update-post [post]
  (when (and (:title post) (:id post))
    (delete-post (:id post))
    (add-post post)))

(defn search
  [q & {:keys [limit]}]
  (let [limit (if limit limit 5)]
    (lucene/search @index-store
                   q
                   limit ; max-num
                   analyzer
                   0 ; page
                   limit))) ; max-num-per-page

(defn prefix-search
  [q {:keys [limit]
      :or {limit 5}
      :as opts}]
  (let [[k v]
        (cond
          (:screen_name q)
          ["screen_name" (:screen_name q)]

          (:post_title q)
          ["post_title" (:post_title q)])]
    (let [term (Term. k v)
          prefix-q (PrefixQuery. term)
          result (apply search prefix-q opts)]
      (if (:screen_name q)
        (mapv :screen_name result)
        result))))
