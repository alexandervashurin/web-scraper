(ns web-scraper.export
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [cheshire.core :as json])) ;; <-- Библиотека для JSON

(defn to-csv [filename data]
  "Экспорт в CSV файл"
  (if (empty? data)
    (println "[Export] Нет данных для CSV.")
    (with-open [writer (io/writer filename)]
      (let [headers (map name (keys (first data)))]
        (csv/write-csv writer
                       (cons headers
                             (map #(map % (keys (first data))) data))))
      (println (str "[Export] Данные сохранены в " filename)))))

(defn to-json [filename data]
  "Экспорт в JSON файл с форматированием"
  (if (empty? data)
    (println "[Export] Нет данных для JSON.")
    (let [json-str (json/generate-string data {:pretty true})]
      (spit filename json-str)
      (println (str "[Export] Данные сохранены в " filename)))))
