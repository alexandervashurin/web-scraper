(ns web-scraper.parser
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as hs]
            [etaoin.api :as e]
            [clojure.string :as str]))

;; Вспомогательная функция: конвертирует любой контент в строку
(defn- content-to-string [content]
  (cond
    (nil? content) ""
    (string? content) content
    (char? content) (str content)
    (sequential? content) (apply str (map content-to-string content))
    :else (str content)))

;; Улучшенная функция извлечения
(defn- extract-from-html [body]
  (let [parsed (hickory/as-hickory (hickory/parse body))
        
        ;; Заголовок: пробуем разные варианты
        h1-nodes (hs/select (hs/tag :h1) parsed)
        h2-nodes (hs/select (hs/tag :h2) parsed)
        title-node (or (first h1-nodes) (first h2-nodes))
        title (if title-node
                (-> title-node :content content-to-string str/trim)
                "N/A")
        
        ;; Контент: собираем все абзацы
        p-nodes (hs/select (hs/tag :p) parsed)
        paragraphs (map #(-> % :content content-to-string str/trim) p-nodes)
        content (if (seq paragraphs)
                  (str/join "\n\n" (filter seq? paragraphs))
                  "N/A")]
    
    {:title title
     :content content}))

(defn fetch-static [url]
  "Парсинг без исполнения JS"
  (try
    (println (str "   [HTTP] Запрос: " url))
    (let [{:keys [status body]} (http/get url {:accept-encoding ["gzip" "deflate"] 
                                                :throw-exceptions false
                                                :timeout 10000})]
      (if (= 200 status)
        (let [data (extract-from-html body)]
          (println (str "   ✓ Заголовок: " (subs (:title data) 0 (min 40 (count (:title data)))) "..."))
          {:source url
           :type "static"
           :title (or (:title data) "N/A")
           :content (or (:content data) "N/A")})
        (do
          (println (str "   ✗ HTTP " status))
          nil)))
    (catch Exception e
      (println (str "   ✗ Ошибка: " (.getMessage e)))
      nil)))

(defn fetch-dynamic [url]
  "Парсинг с исполнением JS (требуется chromedriver)"
  (try
    (println (str "   [браузер] Открываем: " url))
    (let [driver (e/chrome {:args ["--headless" "--no-sandbox" "--disable-dev-shm-usage"]})]
      (e/go driver url)
      
      ;; Пробуем разные селекторы для заголовка
      (let [title-el (or (first (e/query driver [:tag :h1]))
                         (first (e/query driver [:tag :h2]))
                         (first (e/query driver [:tag :title])))
            ;; Контент: пробуем разные варианты
            content-el (or (first (e/query driver [:tag :p]))
                           (first (e/query driver [:class "content"]))
                           (first (e/query driver [:class "main"])))
            title (when title-el (e/get-element-text-el driver title-el))
            content (when content-el (e/get-element-text-el driver content-el))]
        (e/quit driver)
        (println (str "   ✓ Заголовок: " (subs (or title "N/A") 0 (min 40 (count (or title "N/A")))) "..."))
        {:source url
         :type "dynamic"
         :title (or (str/trim (str title)) "N/A")
         :content (or (str/trim (str content)) "N/A")}))
    (catch Exception e
      (println (str "   ✗ Ошибка: " (.getMessage e)))
      nil)))