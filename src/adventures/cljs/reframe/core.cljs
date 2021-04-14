

;; ------- SETUP -------
(ns adventures.cljs.reframe.core
  (:require
   [reagent.dom :refer [render]]
   [re-frame.core :as rf]
   [adventures.cljs.reframe.usecases :as ccases]
   [adventures.cljs.reframe.routes :as routes]
   [adventures.cljs.reframe.views.home :as views]
   [adventures.cljs.reframe.config :as config]
   [adventures.cljs.reframe.styles :as styl]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (println "mount")
  (rf/clear-subscription-cache!)
  (styl/inject-trace-styles js/document)
  (render [views/main-panel]
          (.getElementById js/document "app")))

(defn ^:after-load re-render []
  (mount-root))

(defn ^:export init []
  (println "init again..")
  (rf/dispatch-sync [::ccases/initialize-db])
  (dev-setup)
  (routes/app-routes)

  (mount-root))

;;(defonce init-block (init))
