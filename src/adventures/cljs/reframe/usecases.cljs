
;; ------- USE-CASES -------
(ns adventures.cljs.reframe.usecases
  (:require
   [re-frame.core :as rf]
   [adventures.cljs.reframe.db :as db]
   [adventures.cljs.reframe.tools :refer [sdb gdb]]))

(rf/reg-sub ::name (gdb [:name]))
(rf/reg-sub ::active-panel (gdb [:active-panel]))
(rf/reg-sub ::re-pressed-example  (gdb [:re-pressed-example]))

(rf/reg-event-db ::initialize-db (constantly db/default-db))
(rf/reg-event-db ::set-active-panel [rf/debug] (sdb [:active-panel]))
