(ns web-scraper.core
  (:require [web-scraper.parser :as parser]
            [web-scraper.export :as export]
            [web-scraper.config :as config]
            [clojure.string :as str]))

(defn process-target
  "Парсит цель и возвращает результат"
  [{:keys [url type]}]
  (let [clean-url (str/trim url)
        result (case type
                 :static (parser/fetch-static clean-url)
                 :dynamic (parser/fetch-dynamic clean-url)
                 (do
                   (println (str "[Core] Неизвестный тип парсера: " type))
                   nil))]
    (when result
      (assoc result :source clean-url))))

(defn- print-truncated [s max-len]
  "Выводит усеченную строку"
  (let [len (count s)
        display-len (min max-len len)]
    (str (subs s 0 display-len) (when (> len max-len) "..."))))

(defn- run-targets [targets]
  "Обрабатывает список целей"
  (println "\n[START] Обработка URL:")
  (let [results (atom [])]
    (doseq [target targets]
      (println (str "\n  -> " (name (:type target)) ": '" (:url target) "'"))
      (let [result (process-target target)]
        (if result
          (do
            (swap! results conj result)
            (println "     ✓ Успешно")
            (println (str "       Заголовок: " (print-truncated (:title result) 60))))
          (println "     ✗ Не удалось извлечь данные"))))
    @results))

(defn- export-results [results]
  "Экспортирует результаты в файлы"
  (println "\n============================================")
  (println "[START] Экспорт данных:")
  (if (seq results)
    (do
      (export/to-csv "results.csv" results)
      (export/to-json "results.json" results)
      (println (str "\n✅ Успешно обработано " (count results) " URL(ов)"))
      (println "📁 Результаты сохранены в results.csv и results.json"))
    (println "[Export] Нет данных для экспорта.")))

(defn -main [& args]
  (println "============================================")
  (println "Запуск модульной системы парсинга...")
  (println "============================================")

  ;; Цели для парсинга (с пробелами для теста обрезки)
  (let [targets (if (seq args)
                  (config/load-targets)
                  [{:url "https://example.com " :type :static}
                   {:url " https://example.com" :type :dynamic}
                   {:url "https://nweb42.com/books/clojure/  " :type :static}
                   {:url " https://nweb42.com/books/clojure/" :type :dynamic}])
        results (run-targets targets)]
    (export-results results))

  (println "============================================")
  (println "Работа завершена.")
  (println "============================================"))
