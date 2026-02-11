(ns web-scraper.parser
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as hs]
            [etaoin.api :as e]))

;; Вспомогательная функция для извлечения данных из HTML
(defn- extract-from-html [body]
  (let [;; КРИТИЧНО: преобразуем в формат Hickory AST
        parsed (hickory/as-hickory (hickory/parse body))
        h1-nodes (hs/select (hs/tag :h1) parsed)
        p-nodes (hs/select (hs/tag :p) parsed)]
    ;; str защищает от Character (например, \A → "A")
    {:title (some-> h1-nodes first :content first str)
     :content (some-> p-nodes first :content first str)}))

(defn fetch-static [url]
 ;; "Парсинг без исполнения JS"
  (try
    (let [{:keys [status body]} (http/get url {:accept-encoding ["gzip" "deflate"] :throw-exceptions false})]
      (if (= 200 status)
        (let [data (extract-from-html body)]
          {:source url
           :type "static"
           :title (or (:title data) "N/A")
           :content (or (:content data) "N/A")})
        (do
          (println (str "[Parser Static] HTTP " status " для: " url))
          nil)))
    (catch Exception e
      (println (str "[Parser Static] Ошибка: " (.getMessage e)))
      (.printStackTrace e)
      nil)))

(defn fetch-dynamic [url]
  ;;"Парсинг с исполнением JS (требуется chromedriver)"
  (try
    (println (str "[Parser Dynamic] Запуск браузера для: " url))
    (let [driver (e/chrome)]
      (e/go driver url)
      ;; Правильные локаторы: [:tag :h1], а не [:h1]
      (e/wait-exists driver [:tag :h1] 10000)
      ;; Правильная последовательность для etaoin 1.1.43:
      (let [title-el (first (e/query driver [:tag :h1]))
            content-el (first (e/query driver [:tag :p]))
            title (when title-el (e/get-element-text-el driver title-el))
            content (when content-el (e/get-element-text-el driver content-el))]
        (e/quit driver)
        {:source url
         :type "dynamic"
         :title (or title "N/A")
         :content (or content "N/A")}))
    (catch Exception e
      (println (str "[Parser Dynamic] Ошибка: " (.getMessage e)))
      (.printStackTrace e)
      nil)))
