(ns web-scraper.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def config-file "resources/targets.edn")

(defn load-targets []
 ; "Читает список целей из EDN файла"
  (if (.exists (io/file config-file))
    (try
      (edn/read-string (slurp config-file))
      (catch Exception e
        (println (str "[Config] Ошибка чтения файла: " (.getMessage e)))
        []))
    (do
      (println (str "[Config] Файл " config-file " не найден. Создайте его."))
      [])))
