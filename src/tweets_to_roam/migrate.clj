(ns tweets-to-roam.migrate
  (:require [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [java-time :as time]))

(defn fucking-locale-in-clojure [month]
  ((keyword month) {:janeiro "january"
                    :fevereiro "february"
                    :março "march"
                    :abril "april"
                    :maio "may"
                    :junho "june"
                    :julho "july"
                    :agosto "august"
                    :setembro "september"
                    :outubro "october"
                    :novembro "november"
                    :dezembro "december"}))

(defn translate-day [day]
  (cond
    (= day "1") "1st"
    (= day "2") "2nd"
    (= day "3") "3rd"
    :else (str day "th")))

(defn translate [month]
  (string/replace month
                  #"([a-zç]*)\ ([0-9]*), ([0-9]*)"
                  #(str (fucking-locale-in-clojure (%1 1)) " " (translate-day (%1 2)) ", " (%1 3))))

(defn java-instant->to-format
  "Format a Java instant"
  [date format]
  (let [formatter         (time/format format)
        instant-with-zone (.atZone date (time/zone-id))]
    (time/format formatter instant-with-zone)))

(defn parse-timestamp [timestamp]
  (java.time.Instant/ofEpochSecond (Long/parseLong timestamp)))

(defn timestamp->roam-date-format [timestamp]
  (let [parsed-timestamp (parse-timestamp timestamp)]
    (-> parsed-timestamp
        (java-instant->to-format "MMMM d, YYYY")
        (translate)
        (string/capitalize))))

(defn timestamp->hour-minute [timestamp]
  (let [parsed-timestamp (parse-timestamp timestamp)]
    (-> parsed-timestamp
        (java-instant->to-format "HH:mm"))))

(defn raw-map->roam-map [{:keys [timestamp tweet]}]
  {:title (timestamp->roam-date-format timestamp)
   :children [{:string (str (timestamp->hour-minute timestamp) " " tweet)}]})

(defn merge-roam-maps
  [{first-title :title :as first-map}
   {second-title :title :as second-map}]
  (if (= first-title second-title)
    (update first-map :children concat (:children second-map))
    [first-map second-map]))

(defn csv->map [csv-string]
  (let [parsed-csv (csv/read-csv csv-string)]
    (map zipmap
         (->> (first parsed-csv)
              (map keyword)
              repeat)
         (rest parsed-csv))))

(defn append-at-end [tweet hashtag]
  (str tweet " " hashtag))

(defn append-my-custom-page-tag [raw-map]
  (update raw-map :tweet #(append-at-end % "#[[My Tweets]]")))

(defn build-roam-map [maps]
  (reduce (fn [final current-map]
            (let [pred #(= (:title %1) (:title current-map))]
              (if-let [founded (first (filter pred final))]
                (->> final
                     (remove pred)
                     (cons (update founded :children concat (:children current-map))))
                (conj final current-map))))
          []
          maps))

(defn run [file-path]
  (let [file (slurp file-path)]
    (->> file
         csv->map
         (map append-my-custom-page-tag)
         (map raw-map->roam-map)
         build-roam-map
         json/write-str
         println)))
