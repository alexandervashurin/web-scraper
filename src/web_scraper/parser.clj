(ns web-scraper.parser
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as hs]
            [etaoin.api :as e]
            [clojure.string :as str]))

;; Улучшенная функция извлечения: ищем контент в разных местах
(defn- extract-from-html [body]
  (let [parsed (hickory/as-hickory (hickory/parse body))
        ;; Заголовок: первый h1 или title
        h1-nodes (hs/select (hs/tag :h1) parsed)
        title (or (some-> h1-nodes first :content (apply str) str/trim)
                  "N/A")
        
        ;; Контент: пробуем разные стратегии
        ;; Стратегия 1: все абзацы <p>
        p-nodes (hs/select (hs/tag :p) parsed)
        paragraphs (map #(some-> % :content (apply str) str/trim) p-nodes)
        content-from-p (when (seq paragraphs) (str/join "\n\n" (filter seq? paragraphs)))
        
        ;; Стратегия 2: основной контент в <div class="content">
        content-divs (hs/select (hs/and (hs/tag :div) (hs/class "content")) parsed)
        content-from-div (some-> content-divs first :content (apply str) str/trim)
        
        ;; Стратегия 3: первый большой блок текста
        all-divs (hs/select (hs/tag :div) parsed)
        big-divs (filter #(> (count (str (:content % ""))) 50) all-divs)
        content-from-big-div (some-> big-divs first :content (apply str) str/trim)]
    
    {:title (if (= title "N/A") "N/A" title)
     :content (or content-from-p content-from-div content-from-big-div "N/A")}))

(defn fetch-static [url]
  "Парсинг без исполнения JS"
  (try
    (let [{:keys [status body]} (http/get url {:accept-encoding ["gzip" "deflate"] 
                                                :throw-exceptions false
                                                :timeout 10000})]
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
      nil)))

(defn fetch-dynamic [url]
  "Парсинг с исполнением JS (требуется chromedriver)"
  (try
    (println (str "   [браузер] Открываем: " url))
    (let [driver (e/chrome {:args ["--headless" "--no-sandbox" "--disable-dev-shm-usage"]})]
      (e/go driver url)
      ;; Исправлено: wait-exists без таймаута (в etaoin 1.1.43 таймаут передаётся иначе)
      (e/wait-exists driver [:tag :h1])
      
      (let [title-el (first (e/query driver [:tag :h1]))
            ;; Ищем контент в нескольких местах
            p-el (first (e/query driver [:tag :p]))
            content-el (or (first (e/query driver [:class "content"]))
                           (first (e/query driver [:class "main"]))
                           p-el)
            title (when title-el (e/get-element-text-el driver title-el))
            content (when content-el (e/get-element-text-el driver content-el))]
        (e/quit driver)
        {:source url
         :type "dynamic"
         :title (or (str/trim (str title)) "N/A")
         :content (or (str/trim (str content)) "N/A")}))
    (catch Exception e
      (println (str "[Parser Dynamic] Ошибка: " (.getMessage e)))
      nil)))