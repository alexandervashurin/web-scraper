(ns web-scraper.export
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(defn to-csv
  "Экспорт в CSV файл"
  [filename data]
  (if (empty? data)
    (println "[Export] Нет данных для CSV.")
    (try
      (with-open [writer (io/writer filename)]
        (let [headers (map name (keys (first data)))]
          (csv/write-csv writer
                         (cons headers
                               (map #(map % (keys (first data))) data)))))
      (println (str "[Export] Данные сохранены в " filename))
      (catch Exception e
        (println (str "[Export] Ошибка записи CSV: " (.getMessage e)))))))

(defn to-json
  "Экспорт в JSON файл с форматированием"
  [filename data]
  (if (empty? data)
    (println "[Export] Нет данных для JSON.")
    (try
      (spit filename (json/generate-string data {:pretty true}))
      (println (str "[Export] Данные сохранены в " filename))
      (catch Exception e
        (println (str "[Export] Ошибка записи JSON: " (.getMessage e)))))))
