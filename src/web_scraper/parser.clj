(ns web-scraper.parser
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as hs]
            [etaoin.api :as e]))

(defn- extract-from-html [body]
  (try
    (let [parsed (hickory/parse body)
          ;; Явно указываем вектор селекторов: [:tag :h1]
          ;; Это наиболее надежный способ в Hickory
          h1-nodes (hs/select [:tag :h1] parsed)
          p-nodes (hs/select [:tag :p] parsed)]

      ;; Извлекаем текст с защитой от nil
      {:title (if (seq h1-nodes)
                (first (:content (first h1-nodes)))
                "No Title Found")
       :content (if (seq p-nodes)
                  (first (:content (first p-nodes)))
                  "No Content Found")})
    (catch Exception e
      (println "[Parser] Ошибка парсинга HTML:" (.getMessage e))
      {:title "Parse Error" :content "Parse Error"})))

(defn fetch-static [url]
  "Парсинг без исполнения JS"
  (try
    (println (str "[Parser Static] Загрузка: " url))
    ;; Добавим User-Agent, так как некоторые сайты блокируют запросы по умолчанию
    (let [{:keys [status body]} (http/get url
                                          {:accept-encoding "gzip, deflate"
                                           :headers {"User-Agent" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"}})]
      (if (= 200 status)
        (let [data (extract-from-html body)]
          {:source url
           :type "static"
           :title (or (:title data) "N/A")
           :content (or (:content data) "N/A")})
        (throw (ex-info "Bad HTTP status" {:url url :status status}))))
    (catch Exception e
      (println (str "[Parser Static] Критическая ошибка: " (.getMessage e)))
      nil)))

(defn fetch-dynamic [url]
  "Парсинг с исполнением JS (требуется chromedriver)"
  (try
    (println (str "[Parser Dynamic] Запуск браузера для: " url))
    (let [driver (e/chrome)]
      (e/go driver url)
      ;; Ждем H1 максимум 10 секунд
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
