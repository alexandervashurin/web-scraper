(ns web-scraper.python-bridge
  (:require [clojure.string :as str]
            [cheshire.core :as json])
  (:import [java.io BufferedReader InputStreamReader]
           [java.lang Process]))

(defn- read-stream [^java.io.InputStream stream]
  "Читает InputStream в строку"
  (with-open [reader (BufferedReader. (InputStreamReader. stream))]
    (slurp reader)))

(defn run-script
  "Запускает python3 скрипт, передает URL, читает stdout.
   Ожидает JSON в stdout от скрипта."
  [^String script-path ^String url]
  (let [proc (atom nil)]
    (try
      (let [cmd (format "python3 %s %s" script-path url)]
        (reset! proc (.exec (Runtime/getRuntime) cmd))
        (let [exit-code (.waitFor ^Process @proc)
              stdout (read-stream (.getInputStream ^Process @proc))
              stderr (read-stream (.getErrorStream ^Process @proc))]
          (if (zero? exit-code)
            (try
              (let [data (json/parse-string stdout true)]
                (assoc data :type "python-hybrid" :source url))
              (catch Exception e
                (println (str "[Python] Ошибка парсинга JSON: " (.getMessage e)))
                {:source url
                 :type "python-hybrid"
                 :title (str/trim-newline stdout)
                 :content "Parsed via Python Bridge"}))
            (do
              (println (str "[Python] Скрипт вернул ошибку (exit " exit-code "): " stderr))
              nil))))
      (catch Exception e
        (println (str "[Python] Ошибка запуска: " (.getMessage e)))
        nil)
      (finally
        (when @proc
          (.destroy ^Process @proc))))))
