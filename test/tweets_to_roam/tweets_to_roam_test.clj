(ns tweets-to-roam.tweets-to-roam-test
  (:require [clojure.test :refer :all]
            [tweets-to-roam.tweets-to-roam :refer :all]))

(deftest main
  (testing "returns nil"
    (is (nil? (-main "/dev/null")))))
