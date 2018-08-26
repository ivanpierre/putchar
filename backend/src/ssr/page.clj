(ns ssr.page
  (:import [java.io ByteArrayOutputStream])
  (:require [hiccup.page :as h]
            [cognitect.transit :as t]
            [clojure.data.json :as json]
            [api.config :refer [config]]
            [api.handler.query :as query]
            [share.util :as util]
            [share.dicts :refer [t]]
            [clojure.java.io :as io]
            [rum.core :as rum]
            [share.version :refer [version]]
            [share.kit.ui :as ui]
            [share.kit.colors :as colors]
            [share.components.widgets :as widgets]
            [share.seo :as seo]
            [api.services.slack :as slack]))

;; encode state hash into Transit format
(defn state->str [state]
  (let [out (ByteArrayOutputStream.)]
    (t/write (t/writer out :json) state)
    (json/write-str (.toString out))))

(def style
  (memoize
   (fn [name]
     (if util/development?
       [:link {:rel "stylesheet"
               :href (str "/css/" name)}]
       (let [content (slurp (io/resource (str "public/" name)))]
         [:style { :type "text/css" :dangerouslySetInnerHTML { :__html content }}])))))

(defonce debug-state (atom nil))

(defn head
  [req handler zh-cn? seo-content seo-title seo-image canonical-url]
  (let [post? (= handler :post)
        group-name (and (= handler :group)
                        (get-in req [:ui/route :route-params :group-name]))]
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:meta {:name "apple-mobile-web-app-capable"
             :content "yes"}]
     [:meta {:name "mobile-web-app-capable"
             :content "yes"}]
     [:meta {:http-equiv "X-UA-Compatible"
             :content "IE=edge"}]

     ;; twitter
     [:meta {:name "twitter:card"
             :content "summary"}]

     [:meta {:name "twitter:description"
             :content seo-content}]

     [:meta {:name "twitter:site"
             :content "@lambdahackers"}]

     [:meta {:name "twitter:title"
             :content seo-title}]

     [:meta {:name "twitter:image:src"
             :content seo-image}]

     [:meta {:name "twitter:image:alt"
             :content seo-title}]

     ;; open graph
     [:meta {:property "og:title"
             :content seo-title}]

     [:meta {:property "og:type"
             :content (if (= handler :post)
                        "article"
                        "site")}]
     [:meta {:property "og:url"
             :content (str (:website-uri config) (:uri req))}]
     [:meta {:property "og:image"
             :content seo-image}]
     [:meta {:property "og:description"
             :content seo-content}]
     [:meta {:property "og:site_name"
             :content "lambdahackers"}]

     [:title seo-title]
     [:meta {:name "description"
             :content seo-content}]
     (when (and post? canonical-url)
       [:link {:rel "canonical"
               :href canonical-url}])

     ;; rss
     ;; group
     (when group-name
       [:link {:rel "alternate"
               :type "application/rss+xml"
               :title (str "Hot posts on " (util/original-name group-name))
               :href (str (:website-uri config) "/" group-name "/hot.rss")}])

     (when group-name
       [:link {:rel "alternate"
               :type "application/rss+xml"
               :title (str "Latest posts on " (util/original-name group-name))
               :href (str (:website-uri config) "/" group-name "/latest-reply.rss")}])

     (when group-name
       [:link {:rel "alternate"
               :type "application/rss+xml"
               :title (str "Newest posts on " (util/original-name group-name))
               :href (str (:website-uri config) "/" group-name "/newest.rss")}])

     [:link {:rel "alternate"
             :type "application/rss+xml"
             :title "Hot posts"
             :href (str (:website-uri config) "/hot.rss")}]

     [:link {:rel "alternate"
             :type "application/rss+xml"
             :title "Latest posts"
             :href (str (:website-uri config) "/latest-reply.rss")}]

     [:link {:rel "alternate"
             :type "application/rss+xml"
             :title "Newest posts"
             :href (str (:website-uri config) "/newest.rss")}]

     [:meta {:name "theme-color"
             :content "#FFFFFF"}]

     [:link
      {:href "/apple-touch-icon.png",
       :sizes "180x180",
       :rel "apple-touch-icon"}]

     [:link
      {:href "/favicon-32x32.png?v=3",
       :sizes "32x32",
       :type "image/png",
       :rel "icon"}]

     [:link
      {:href "/favicon-16x16.png?v=3",
       :sizes "16x16",
       :type "image/png",
       :rel "icon"}]

     [:link
      {:color "#5bbad5",
       :href "/safari-pinned-tab.svg",
       :rel "mask-icon"}]

     [:meta {:content "#1a1a1a", :name "msapplication-TileColor"}]
     [:meta {:content "#ffffff", :name "theme-color"}]

     [:link {:rel "manifest"
             :href "/manifest.json"}]

     (if util/development?
       [:link {:rel "stylesheet"
               :href "/css/style.css"}]
       [:link {:rel "stylesheet"
               :href (str "/style-" version ".css")}])]))

(defn status-template
  [text]
  (h/html5
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport"
               :content "width=device-width, initial-scale=1"}]
       [:meta {:name "apple-mobile-web-app-capable"
               :content "yes"}]
       [:meta {:name "mobile-web-app-capable"
               :content "yes"}]
       [:meta {:http-equiv "X-UA-Compatible"
               :content "IE=edge"}]

       [:link
        {:href "/apple-touch-icon.png",
         :sizes "180x180",
         :rel "apple-touch-icon"}]

       [:link
        {:href "/favicon-32x32.png?v=3",
         :sizes "32x32",
         :type "image/png",
         :rel "icon"}]

       [:link
        {:href "/favicon-16x16.png?v=3",
         :sizes "16x16",
         :type "image/png",
         :rel "icon"}]

       [:link
        {:color "#5bbad5",
         :href "/safari-pinned-tab.svg",
         :rel "mask-icon"}]

       [:meta {:content "#00a300", :name "msapplication-TileColor"}]
       [:meta {:content "#ffffff", :name "theme-color"}]]
      [:body {:style {:height "100%"
                      :width "100%"
                      :display "flex"
                      :flex-direction "column"
                      :margin 0
                      :background-size "cover"
                      :position "relative"
                      :align-items "center"
                      :justify-content "center"
                      :padding-right "0 !important"}}
       [:a {:style {:margin-top "24px"}
            :href "/"}
        [:img {:src "/images/logo.png"}]]
       [:h1 {:style {:margin-top "200px"}}
        text]]))

(defn render-page [content req state]
  (when util/development?
    (reset! debug-state state))

  (if (:not-found state)
    (status-template "404 Not Found")

    (let [locale (:locale state)
         {:keys [handler route-params]} (:ui/route req)
         current-user (get-in state [:user :current])
         [seo-title seo-content canonical-url seo-image] (seo/seo-title-content handler route-params state)
         zh-cn? (= locale :zh-cn)]
     (h/html5
      (head req handler zh-cn? seo-content seo-title seo-image canonical-url)
      [:body {:style {:min-height "100%"}}
       [:div#app content]

       [:script {:src (if util/development?
                        "/js/compiled/main.js"
                        (str "/main-" version ".js"))}]

       [:script {:src "/asciidoctor.min.js"}]

       ;; TODO: http2 push using json
       [:script
        (str "web.core.init(" (state->str state) ")")]

       [:script
        (format
         "
// Check that service workers are registered
if ('serviceWorker' in navigator) {
  // Use the window load event to keep the page load performant
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw%s.js');
  });
}
"
         (if util/development?
           "_dev"
           ""))]

       ;; Google analytics
       [:script {:src "/ga.js"
                 :defer true
                 :async true}]
]))))
