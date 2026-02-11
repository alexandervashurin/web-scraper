(ns web-scraper.core
  (:require [web-scraper.db :as db]
            [web-scraper.parser :as parser]
            [web-scraper.export :as export]
            [web-scraper.python-bridge :as py]))

(defn process-target [target]
;;  "Оркестратор: выбирает парсер и сохраняет в БД"
  (let [{:keys [url type]} target
        result (case type
                 :static (parser/fetch-static url)
                 :dynamic (parser/fetch-dynamic url)
                 :python (py/run-script "resources/scraper.py" url)
                 (do
                   (println (str "[Core] Неизвестный тип парсера: " type))
                   nil))]
    (when result
      (db/insert-result! result))))

(defn -main [& args]
  (println "============================================")
  (println "Запуск модульного системы парсинга...")
  (println "============================================")

  ;; 1. Инициализация БД
  (db/init-table!)

  ;; 2. Список целей для парсинга
  (def targets [{:url "https://nweb42.com/books/" :type :static}
                {:url "https://nweb42.com/books/" :type :dynamic}
                {:url "https://nweb42.com/books/" :type :python}
                ])

  ;; 3. Цикл обработки
  (println "\n[START] Обработка URL:")
  (doseq [target targets]
    (println (str "  -> Обработка: " (:url target) " (" (name (:type target)) ")"))
    (process-target target))

  ;; 4. Экспорт результатов
  (println "\n[START] Экспорт данных:")
  (let [all-data (db/fetch-all-results)]
    (if (seq all-data)
      (do
        (export/to-csv "results.csv" all-data)
        (export/to-json "results.json" all-data))
      (println "[Export] Нет данных для экспорта.")))

  (println "============================================")
  (println "Работа завершена.")
  (println "============================================"))
