(defproject web-scraper "0.1.0-SNAPSHOT"
  :description "Модульный веб-скрапер: Static, Dynamic, SQLite, CSV, JSON"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 
                 ;; HTTP и HTML (Static)
                 [clj-http/clj-http "3.12.3"]
                 [hickory/hickory "0.7.1"]
                 
                 ;; Драйвер браузера (Dynamic)
                 [etaoin/etaoin "1.0.40"]
                 
                 ;; База данных
                 [com.github.seancorfield/next.jdbc "1.3.883"]
                 [org.xerial/sqlite-jdbc "3.42.0.0"]
                 
                 ;; Экспорт
                 [org.clojure/data.csv "1.0.1"]
                 [cheshire "5.11.0"]  ;; <-- Для экспорта в JSON
                 
                 ;; Логирование (опционально)
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.4.11"]]

  :main ^:skip-aot web-scraper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
