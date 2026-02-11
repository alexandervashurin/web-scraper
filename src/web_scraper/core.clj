(ns web-scraper.core
  (:require [web-scraper.parser :as parser]
            [web-scraper.export :as export]
            [clojure.string :as str]))

(defn process-target [target]
;;  "Парсит цель и возвращает результат (обрезаем пробелы из URL)"
  (let [{:keys [url type]} target
        clean-url (str/trim url)
        result (case type
                 :static (parser/fetch-static clean-url)
                 :dynamic (parser/fetch-dynamic clean-url)
                 (do
                   (println (str "[Core] Неизвестный тип парсера: " type))
                   nil))]
    (when result
      (assoc result :source clean-url)))) ; сохраняем очищенный URL

(defn -main [& args]
  (println "============================================")
  (println "Запуск модульного системы парсинга...")
  (println "============================================")

  ;; Список целей для парсинга (с пробелами для теста обрезки)
  (def targets [{:url "https://nweb42.com/books/clojure/ " :type :static}
                {:url "https://nweb42.com/books/clojure/ " :type :dynamic}])

  ;; Цикл обработки
  (println "\n[START] Обработка URL:")
  (let [results (atom [])]
    (doseq [target targets]
      (println (str "  -> Обработка: '" (:url target) "' (" (name (:type target)) ")"))
      (let [result (process-target target)]
        (if result
          (do
            (swap! results conj result)
            (println (str "     ✓ Успешно: " (subs (:title result) 0 (min 50 (count (:title result)))) "...")))
          (println "     ✗ Ошибка: не удалось извлечь данные"))))

    ;; Экспорт результатов
    (println "\n[START] Экспорт данных:")
    (if (seq @results)
      (do
        (export/to-csv "results.csv" @results)
        (export/to-json "results.json" @results)
        (println (str "\n✅ Успешно обработано " (count @results) " URL(ов)"))
        (println "📁 Результаты сохранены в results.csv и results.json"))
      (println "[Export] Нет данных для экспорта.")))

  (println "============================================")
  (println "Работа завершена.")
  (println "============================================"))
