(ns web-scraper.python-bridge
  (:require [clojure.string :as str]))

(defn run-script [script-path url]
  "Запускает python3 скрипт, передает URL, читает stdout"
  (try
    (let [cmd (format "python3 %s %s" script-path url)
          proc (.exec (Runtime/getRuntime) cmd)
          exit-code (.waitFor proc)
          stdout (slurp (.getInputStream proc))]
      (if (zero? exit-code)
        ;; В реальном проекте здесь нужно распарсить JSON stdout через cheshire
        {:source url
         :type "python-hybrid"
         :title (str/trim-newline stdout)
         :content "Parsed via Python Bridge"}
        (do
          (println (str "[Python] Скрипт вернул ошибку: " (slurp (.getErrorStream proc))))
          nil)))
    (catch Exception e
      (println (str "[Python] Ошибка запуска: " (.getMessage e)))
      nil)))
