(ns web-scraper.parser
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as hs]
            [etaoin.api :as e]))

;; Вспомогательная функция для извлечения данных из HTML
(defn- extract-from-html [body]
  (let [parsed (hickory/parse body)]
    {:title (some-> (hs/select (hs/tag :h1) parsed) first :content first)
     :content (some-> (hs/select (hs/tag :p) parsed) first :content first)}))

(defn fetch-static [url]
 ; "Парсинг без исполнения JS"
  (try
    (let [{:keys [status body]} (http/get url {:accept-encoding "gzip, deflate"})]
      (if (= 200 status)
        (let [data (extract-from-html body)]
          {:source url
           :type "static"
           :title (or (:title data) "N/A")
           :content (or (:content data) "N/A")})
        (throw (ex-info "Bad HTTP status" {:url url :status status}))))
    (catch Exception e
      (println (str "[Parser Static] Ошибка: " (.getMessage e)))
      nil)))

(defn fetch-dynamic [url]
  ;"Парсинг с исполнением JS (требуется chromedriver)"
  (try
    (println (str "[Parser Dynamic] Запуск браузера для: " url))
    (let [driver (e/chrome)]
      (e/go driver url)
      ;; Исправлено: wait-exist -> wait-exists
      (e/wait-exists driver [:h1] 10000)

      (let [title (e/get-element-text-el driver (e/query driver [:h1]))
            content (e/get-element-text-el driver (e/query driver [:p]))]
        (e/quit driver)
        {:source url
         :type "dynamic"
         :title title
         :content content}))
    (catch Exception e
      (println (str "[Parser Dynamic] Ошибка: " (.getMessage e)))
      nil)))
