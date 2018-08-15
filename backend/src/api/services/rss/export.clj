(ns api.services.rss.export
  (:use [clojure.xml :only [emit]]
        [clojure.set :only [difference]]
        [clojure.string :only [escape join]])
  (:import java.util.Date java.util.Locale java.text.SimpleDateFormat))

;; copy from https://github.com/yogthos/clj-rss/blob/master/src/clj_rss/core.clj
(defn- xml-str
  "Returns a string suitable for inclusion as an XML element. If the string
  is wrapped in <![CDATA[ ... ]]>, do not escape special characters."
  [^String s]
  (if (and (.startsWith s "<![CDATA[")
           (.endsWith s "]]>"))
    s
    (if s
      (let [escapes {\< "&lt;",
                     \> "&gt;",
                     \& "&amp;",
                     \" "&quot;"}]
        (escape s escapes)))))

(defmacro tag [id & xs]
  `(let [attrs# (map? (first '~xs))
         content# [~@xs]]
     {:tag ~id
      :attrs (if attrs# (first '~xs))
      :content (if attrs# (rest content#) content#)}))

(defmacro functionize [macro]
  `(fn [& args#] (eval (cons '~macro args#))))

(defmacro apply-macro [macro args]
  `(apply (functionize ~macro) ~args))

(defn dissoc-nil [map]
  "Returns a map containing only those entries in m whose val is not nil"
  (let [non-nil-keys (for [[k v] map :when (not (nil? v))] k)]
    (select-keys map non-nil-keys)))

(defn- validate-tags [tags valid-tags]
  (let [diff (difference (set tags) valid-tags)]
    (when (not-empty diff)
      (throw (new Exception (str "unrecognized tags in channel: " (join ", " diff)))))))

(defn- validate-channel [tags & ks]
  (doseq [k ks]
    (or (get tags k) (throw (new Exception (str k " is a required element")))))
  (validate-tags (keys tags)
                 #{:title
                   :link
                   :description
                   :category
                   :cloud
                   :copyright
                   :docs
                   :image
                   :language
                   :lastBuildDate
                   :managingEditor
                   :pubDate
                   :rating
                   :skipDays
                   :skipHours
                   :ttl
                   :webMaster}))

(defn- validate-item [tags]
  (if (not (or (:title tags) (:description tags)))
    (throw (new Exception (str "item " tags " must contain one of title or description!"))))
  (validate-tags (keys tags) #{:title :link :description :author :category :comments :enclosure :guid :pubDate :source}))



(defn- make-tags [tags]
  (flatten
   (for [[k v] (seq tags)]
     (cond
       (and (coll? v) (map? (first v)))
       (apply-macro clj-rss.core/tag (into [k] v))
       (coll? v)
       (map (fn [v] (make-tags {k v})) v)
       :else
       (tag k (cond
                (some #{k} [:pubDate :lastBuildDate]) v
                (some #{k} [:description :title :link :author]) (xml-str v)
                :else v))))))


(defn- item [validate? tags]
  (when validate? (validate-item (dissoc-nil tags)))
  {:tag :item
   :attrs nil
   :content (make-tags (dissoc-nil tags))})

(defn- channel'
  "channel accepts a map of tags followed by 0 or more items
  channel:
  required tags: title, link, description
  optional tags: category, cloud, copyright, docs, image, language, lastBuildDate, managingEditor, pubDate, rating, skipDays, skipHours, textInput, ttl, webMaster
  item:
  optional tags: title, link, description, author, category, comments, enclosure, guid, pubDate, source
  one of title or description is required!
  tags can either be strings, dates, or collections
  :title \"Some title\"
  {:tag :title :attrs nil :content [\"Some title\"]}
  :category [{:domain \"http://www.foo.com/test\"} \"TEST\"]
  {:tag :category
   :attrs {:domain \"http://www.fool.com/cusips\"}
   :content (\"MSFT\")}
  official RSS specification: http://cyber.law.harvard.edu/rss/rss.html"
  [validate? tags & items]
  (when validate? (validate-channel tags :title :link :description))
  {:tag :rss
   :attrs {:version "2.0"
           "xmlns:atom" "http://www.w3.org/2005/Atom"}
   :content
   [{:tag :channel
     :attrs nil
     :content (concat
               [{:tag "atom:link"
                 :attrs {:href (:link tags)
                         :rel "self"
                         :type "application/rss+xml"}}]
               (make-tags (conj tags {:generator "clj-rss"}))
               (->> items
                    flatten
                    (map dissoc-nil)
                    (map (partial item validate?))))}]})

(defn channel [& content]
  (cond
    (every? #(if (coll? %) (empty? %) (nil? %)) content)
    (channel' false nil)
    (map? (first content))
    (apply channel' (cons true content))
    :else
    (apply channel' content)))

(defn channel-xml
  "channel accepts a map of tags followed by 0 or more items and outputs an XML string, see channel docs for detailed description"
  [& content]
  (with-out-str (emit (apply channel content))))

(comment
  (channel-xml {:title "Foo" :link "http://foo/bar" :description "some channel"}
               [{:title "Foo"} {:title "Bar"} {:title "Baz"}])

  (let [items [{:title "Foo"} {:title "Bar"} {:title "Baz"}]]
    (channel {:title "Foo" :link "http://foo/bar" :description "some channel"}
             items)))
