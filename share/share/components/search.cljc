(ns share.components.search
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [share.util :as util]
            [appkit.citrus :as citrus]
            [share.components.post :as post]
            [share.dicts :refer [t] :as dicts]))

(rum/defc search < rum/reactive
  []
  (let [{:keys [loading? result q]} (citrus/react [:search])
        current-path (citrus/react [:router :handler])]
    (cond
      (and (string? q) (<= (count q) 1))
      [:div.column]

      :else
      [:div.column
       [:h1.auto-padding {:style {:font-size 36
                                  :margin-top 12
                                  :margin-bottom 24}}
        (t :search-result)]
       (if loading?
         [:div.center {:style {:margin "24px 0"}}
          [:div.spinner]]
         (post/post-list {:result (:posts result)
                          :end? true}
                         {}
                         :empty-widget [:div (t :empty-search-result)]))])))
