(ns tweets-to-roam.tweets-to-roam
  (:gen-class)
  (:require [tweets-to-roam.migrate :as migrate]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (migrate/run (first args)))
