
;; ------- ROUTES -------

(ns adventures.cljs.reframe.routes
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rtfe]
   [reitit.frontend :as rtf]
   [reitit.coercion.schema :as rsc]
   [adventures.cljs.reframe.views.compo :as compo]
   [adventures.cljs.reframe.views.home :as home]
   [adventures.cljs.reframe.usecases :as ccases]))


(def routes
  (rtf/router
   ["/"
    [""
     {:name :routes/frontpage
      :view #'home/main}]
    ["component"
     {:name :routes/component
      :view #'compo/main}]]

   {:data {:coercion rsc/coercion}}))




(defn app-routes []
  (rtfe/start! routes
               (fn [m] (rf/dispatch [::ccases/set-active-panel m]))
               {:use-fragment true}))
