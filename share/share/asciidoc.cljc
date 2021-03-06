(ns share.asciidoc
  (:require #?(:clj [api.asciidoc])
            #?(:cljs [goog.net.jsloader :as jsloader])
            #?(:cljs [goog.html.TrustedResourceUrl :as url])
            #?(:cljs [goog.string.Const :as sc])
            #?(:cljs [web.loader :as loader])
            [share.config :as config]
            [clojure.string :as s])
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go]])))

(defn loaded? []
  #?(:cljs js/window.Asciidoctor
     :clj true))

(defn load []
  #?(:cljs
     (loader/load (str config/website "/asciidoctor.min.js"))))

(defn render [str]
  #?(:clj
     (api.asciidoc/render str)
     :cljs
     (let [opts (clj->js {:attributes {:showTitle false
                                       :hardbreaks true
                                       :icons "font"
                                       :source-highlighter "pygments"}})]
       (when (loaded?)
         (.convert (js/window.Asciidoctor) str opts)))))
