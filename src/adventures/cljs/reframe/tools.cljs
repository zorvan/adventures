
;; ------- TOOLS -------
(ns adventures.cljs.reframe.tools)

(defn sdb [path]
  (fn [db [_ v]]
    (assoc-in db path v)))

(defn gdb
  [path]
  (fn [db _] (get-in db path)))
