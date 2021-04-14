(ns adventures.cljs.reframe.views.home
  (:require
   [re-frame.core :as rf]
   [adventures.cljs.reframe.viewtools :as vt]
   [clojure.pprint :as pp]
    [adventures.cljs.reframe.usecases :as ccases]))

(defn main []
  [:div
   [:h2.text-4xl "home"]
   [:p "nothing to see here"]])

(def toolbar-items
  [["#" :routes/frontpage]
   ["component" :routes/component]])

(defn route-info [route]
  [:div.m-4
   [:p "Routeinfo"]
   [:pre.border-solid.border-2.rounded
    (with-out-str (pp/pprint route))]])
;; main

(defn show-panel [route]
  (when-let [route-data (:data route)]
    (let [view (:view route-data)]
      [:<>
       [view]
       [route-info route]])))

(defn main-panel []
  (let [active-route (rf/subscribe [::ccases/active-panel])]
    [:div
     [vt/navigation toolbar-items]
     [show-panel @active-route]]))
