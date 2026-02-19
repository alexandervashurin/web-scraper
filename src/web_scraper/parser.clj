(ns web-scraper.parser
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as hs]
            [etaoin.api :as e]
            [clojure.string :as str]))

(def ^:private ^:const timeout-ms 10000)

;; Улучшенная функция извлечения
(defn- extract-from-html [body]
  (let [parsed (hickory/as-hickory (hickory/parse body))

        ;; Заголовок: пробуем разные варианты
        h1-nodes (hs/select (hs/tag :h1) parsed)
        h2-nodes (hs/select (hs/tag :h2) parsed)
        title-node (or (first h1-nodes) (first h2-nodes))
        title (if title-node
                (-> title-node :content first str str/trim)
                "N/A")

        ;; Контент: собираем все абзацы
        p-nodes (hs/select (hs/tag :p) parsed)
        paragraphs (keep #(when-let [text (-> % :content first)]
                            (when (string? text)
                              (str/trim text))) p-nodes)
        content (if (seq paragraphs)
                  (str/join "\n\n" paragraphs)
                  "N/A")]

    {:title title
     :content content}))

(defn fetch-static [url]
  "Парсинг без исполнения JS"
  (try
    (println (str "   [HTTP] Запрос: " url))
    (let [{:keys [status body]} (http/get url {:accept-encoding ["gzip" "deflate"]
                                                :throw-exceptions false
                                                :timeout timeout-ms})]
      (if (= 200 status)
        (let [data (extract-from-html body)]
          (println (str "   ✓ Заголовок: " (subs (:title data) 0 (min 40 (count (:title data)))) "..."))
          {:source url
           :type "static"
           :title (:title data "N/A")
           :content (:content data "N/A")})
        (do
          (println (str "   ✗ HTTP " status))
          nil)))
    (catch Exception e
      (println (str "   ✗ Ошибка: " (.getMessage e)))
      nil)))

(defn fetch-dynamic [url]
  "Парсинг с исполнением JS (требуется chromedriver)"
  (let [driver (atom nil)]
    (try
      (println (str "   [браузер] Открываем: " url))
      (reset! driver (e/chrome {:args ["--headless" "--no-sandbox" "--disable-dev-shm-usage"]}))
      (e/go @driver url)
      (e/wait @driver 5)  ;; Ждём загрузки страницы

      ;; Извлекаем текст через get-element-text
      (let [h1-el (e/query @driver "h1")
            h2-el (e/query @driver "h2")
            p-els (e/query @driver "p")
            title (cond
                    (seq h1-el) (e/get-element-text @driver (first h1-el))
                    (seq h2-el) (e/get-element-text @driver (first h2-el))
                    :else "N/A")
            content (if (and p-els (seq p-els))
                      (e/get-element-text @driver (first p-els))
                      "N/A")]
        {:source url
         :type "dynamic"
         :title (str/trim (str title))
         :content (str/trim (str content))})
      (catch Exception e
        (println (str "   ✗ Ошибка: " (.getMessage e)))
        nil)
      (finally
        (when @driver
          (try
            (e/quit @driver)
            (catch Exception _
              (println "   [Warning] Не удалось закрыть драйвер"))))))))