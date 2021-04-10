(ns tweets-to-roam.migrate-test
  (:require [tweets-to-roam.migrate :as migrate]
            [clojure.test :refer :all]))

(defn file-sample [] (slurp "test/helpers/tweets.csv"))
(def sample (file-sample))

(deftest translate
  (testing "translates those portuguese dates"
    (is (= "march 1st, 2021" (migrate/translate "março 1, 2021")))
    (is (= "june 2nd, 2012" (migrate/translate "junho 2, 2012")))
    (is (= "december 3rd, 2011" (migrate/translate "dezembro 3, 2011")))
    (is (= "august 20th, 2019" (migrate/translate "agosto 20, 2019")))
    (is (= "september 9th, 2008" (migrate/translate "setembro 9, 2008")))))

(deftest timestamp->roam-date-format
  (testing "returns Month day, Year"
    (is (= "September 9th, 2008" (migrate/timestamp->roam-date-format "1221008518")))))

(deftest timestamp->hour-minute
  (testing "returns Hour:Minute format"
    (is (= "22:01" (migrate/timestamp->hour-minute "1221008518")))))

(deftest csv->map
  (testing "returns a map from a csv string"
    (is (= [{:a "1" :b "2"}, {:a "3" :b "4"}] (migrate/csv->map "a,b\n1,2\n3,4"))))

  (testing "returns an empty list with a non csv string"
    (is (= [] (migrate/csv->map "test"))))

  (testing "parsing the sample"
    (is (= {:timestamp "1220616118" :tweet "Agora sim eu vou ter mias do que um microblog!!"}
           (first (migrate/csv->map sample))))))

(deftest raw-map->roam-map
  (testing "returns a map ready to be transformed into json "
    (is (= {:title "September 9th, 2008"
            :children [{:string "22:01 Agora sim eu vou ter mias do que um microblog!!"}]}
           (migrate/raw-map->roam-map {:timestamp "1221008518" :tweet "Agora sim eu vou ter mias do que um microblog!!"})))))

(deftest merge-roam-maps
  (testing "With same title"
    (is (= {:title "September 9th, 2008"
            :children [{:string "22:01 Agora sim eu vou ter mias do que um microblog!!"}
                       {:string "13:22 aew!!"}]}
           (migrate/merge-roam-maps {:title "September 9th, 2008"
                                     :children [{:string "22:01 Agora sim eu vou ter mias do que um microblog!!"}]}
                                    {:title "September 9th, 2008"
                                     :children [{:string "13:22 aew!!"}]}))))

  (testing "With different titles"
    (is (= [{:title "September 9th, 2008"
             :children [{:string "22:01 Agora sim eu vou ter mias do que um microblog!!"}]}
            {:title "September 10th, 2008"
             :children [{:string "13:22 aew!!"}]}]
           (migrate/merge-roam-maps {:title "September 9th, 2008"
                                     :children [{:string "22:01 Agora sim eu vou ter mias do que um microblog!!"}]}
                                    {:title "September 10th, 2008"
                                     :children [{:string "13:22 aew!!"}]})))))

(deftest build-roam-map
  (testing "build the map merging children from the same title"
    (let [raw-map (->> sample (migrate/csv->map) (map migrate/raw-map->roam-map))]
      (is (= [{:children [{:string "21:56 Os novos modelos são lindos!"}
                          {:string "22:01 Testando o PingFire para ser postado em meu blog :)"}],
               :title "September 9th, 2008"}
              {:children [{:string "09:01 Agora sim eu vou ter mias do que um microblog!!"}],
               :title "September 5th, 2008"}
              {:children [{:string "22:27 Novidades no meu blog: http://ping.fm/SQQn1"}],
               :title "September 8th, 2008"}]
             (migrate/build-roam-map raw-map))))))

(deftest append-my-custom-page-tag
  (testing "add my page tag at the end of the tweet"
    (is (= {:tweet "whatever #[[My Tweets]]"}
           (migrate/append-my-custom-page-tag {:tweet "whatever"})))))

(deftest run
  (testing "returns nil"
    (is (nil? (migrate/run "/dev/null"))))

  (testing "the json output"
    (is (= (slurp "test/helpers/tweets.json")
           (with-out-str (migrate/run "test/helpers/tweets.csv"))))))
