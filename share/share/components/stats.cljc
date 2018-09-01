(ns share.components.stats
  (:require [rum.core :as rum]
            [share.kit.ui :as ui]
            [appkit.citrus :as citrus]
            [share.kit.colors :as colors]
            [share.util :as util]
            [share.kit.mixins :as mixins]
            [share.kit.query :as query]
            [share.dicts :refer [t] :as dicts]
            [clojure.string :as str]))

(rum/defc stats-cp < rum/static
  [{:keys [views reads]}]
  [:div {:style {:margin-top 24
                 :margin-left 12}}
   [:h6 (t :recent-7-days)]
   (for [[idx [v r]] (util/indexed (reverse (zipmap views reads)))]
     [:p
      (util/format
       "%s: %d %s, %d %s."
       (util/days-ago (inc idx))
       v (t :views) r (t :reads))])])

(rum/defcs item-cp < rum/static
  (rum/local false ::show?)
  [state mobile? {:keys [post_id post_title stats views reads]}]
  (let [show? (get state ::show?)
        on-click-fn #(swap! show? not)
        stats (if stats (util/read-string stats) stats)]
    (if mobile?
      [:div.column {:on-click on-click-fn}
       [:h4 post_title]
       [:div.row1
        [:span {:style {:font-weight "bold"
                        :color (colors/shadow)}}
         views]
        [:span {:style {:margin-left 6}}
         (t :views)]
        [:span {:style {:font-weight "bold"
                        :color (colors/shadow)
                        :margin-left 12}}
         reads]
        [:span {:style {:margin-left 6}}
         (t :reads)]]

       (when @show?
         (stats-cp stats))

       (when @show?
         [:div.divider])]
      [:tr {:key post_id}
      [:td
       [:div
        [:a.control {:on-click on-click-fn}
         [:h4 post_title]]

        (when @show?
          (stats-cp stats))]]
      [:td {:style {:text-align "right"}}
       [:span {:style {:font-weight "bold"
                       :color (colors/shadow)}}
        views]]
      [:td {:style {:text-align "right"}}
       [:span {:style {:font-weight "bold"
                       :color (colors/shadow)}}
        reads]]])
    ))

(rum/defc stats < rum/reactive
  (mixins/query :stats)
  [params]
  (let [mobile? (or (util/mobile?) (<= (citrus/react [:layout :current :width]) 768))]
    [:div.column.auto-padding.center#stats {:style {:max-width 1024}}
     [:h1 (t :stats)]
     (query/query
       (let [stats (citrus/react [:stats])]
        (if (seq stats)
          [:div.center
           (let [all-views (apply + (map :views stats))
                 all-reads (apply + (map :reads stats))]
             [:h5 {:style {:margin-top 48
                           :margin-bottom 12
                           :color (colors/shadow)}}
              (str (str/capitalize (t :views)) " ")
              [:span {:style {:margin-left 6}}
               all-views]
              [:span {:style {:margin-left 24}}
               (str (str/capitalize (t :reads)) " ")]
              [:span {:style {:margin-left 6}}
               all-reads]])

           [:div.divider]

           ;; header
           [:table {:style {:margin-top 12
                            :margin-bottom 128}}
            (when-not mobile?
              [:thead
               [:tr
                [:th {:style {:text-align "left"}} (t :posts)]
                [:th {:style {:text-align "right"}} (str/capitalize (t :views))]
                [:th {:style {:text-align "right"}} (str/capitalize (t :reads))]]])
            [:tbody
             (for [item stats]
               (item-cp mobile? item))]]]
          [:h2.ubuntu (t :no-stats-yet)])))]))
