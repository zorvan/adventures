(ns adventures.clj.genart
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as middleware])


(def w 650)
(def h 680)


(def noise-zoom
  "Noise zoom level."
  0.005)


(def palette
  {:name       "purple haze"
   :background [10 10 10]
   :colors     [[32 0 40]
                [82 15 125]
                [99 53 126]
                [102 10 150]
                [132 26 200]
                [165 32 250]
                [196 106 251]]})


(defn particle
  "Creates a particle map."
  [id]
  {:id        id
   :vx        1
   :vy        1
   :size      3
   :direction 0
   :x         (q/random w)
   :y         (q/random h)
   :color     (rand-nth (:colors palette))})


(defn sketch-setup
  "Returns the initial state to use for the update-render loop."
  []
  (apply q/background (:background palette))
  (map particle (range 0 2000)))


(defn position
  "Calculates the next position based on the current, the speed and a max."
  [current delta max]
  (mod (+ current delta) max))


(defn velocity
  "Calculates the next velocity by averaging the current velocity and the added delta."
  [current delta]
  (/ (+ current delta) 2))


(defn direction
  "Calculates the next direction based on the previous position and id of each particle."
  [x y z]
  (* 2
     Math/PI
     (+ (q/noise (* x noise-zoom) (* y noise-zoom))
        (* 0.2 (q/noise (* x noise-zoom) (* y noise-zoom) (* z noise-zoom))))))


(defn sketch-update
  "Returns the next state to render. Receives the current state as a paramter."
  [particles]
  (map (fn [p]
         (assoc p
                :x         (position (:x p) (:vx p) w)
                :y         (position (:y p) (:vy p) h)
                :direction (direction (:x p) (:y p) (:id p))
                :vx        (velocity (:vx p) (Math/cos (:direction p)))
                :vy        (velocity (:vy p) (Math/sin (:direction p)))))
       particles))


(defn sketch-draw
  "Draws the current state to the canvas. Called on each iteration after sketch-update."
  [particles]
  ;;(apply q/background (:background palette))
  (q/no-stroke)
  (doseq [p particles]
    (apply q/fill (:color p))
    (q/ellipse (:x p) (:y p) (:size p) (:size p))))


(q/sketch
    :host "host"
    :size [w h]
    :draw #'sketch-draw
    :setup #'sketch-setup
    :update #'sketch-update
    :middleware [middleware/fun-mode]
    :settings (fn []
                (q/pixel-density 1)
                (q/random-seed 666)
                (q/noise-seed 666)))
