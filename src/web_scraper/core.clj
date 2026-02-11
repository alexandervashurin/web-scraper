(ns web-scraper.core
  (:require [web-scraper.db :as db]
            [web-scraper.parser :as parser]
            [web-scraper.export :as export]
            [web-scraper.python-bridge :as py]
            [web-scraper.config :as config])) ;; <-- Подключаем модуль конфига

(defn process-target [target]
  ;"Оркестратор: выбирает парсер и сохраняет в БД"
  (let [{:keys [url type]} target
        result (case type
                 :static (parser/fetch-static url)
                 :dynamic (parser/fetch-dynamic url)
                 :python (py/run-script "resources/scraper.py" url)
                 nil)]
    (when result
      (db/insert-result! result))))

(defn -main [& args]
  (println "============================================")
  (println "Запуск модульной системы парсинга...")
  (println "============================================")

  ;; 1. Инициализация БД
  (db/init-table!)

  ;; 2. Загрузка целей из внешнего файла
  (println "\n[Config] Загрузка целей...")
  (def targets (config/load-targets))

  (if (empty? targets)
    (println "[Config] Список целей пуст. Проверьте resources/targets.edn")
    (do
      ;; 3. Цикл обработки
      (println (str "[Start] Найдено целей для обработки: " (count targets)))
      (doseq [target targets]
        (println (str "  -> " (:url target) " [" (:type target) "]"))
        (process-target target))

      ;; 4. Экспорт результатов
      (println "\n[Export] Сохранение данных...")
      (let [all-data (db/fetch-all-results)]
        (export/to-csv "results.csv" all-data)
        (export/to-json "results.json" all-data))))

  (println "============================================")
  (println "Работа завершена.")
  (println "============================================"))
