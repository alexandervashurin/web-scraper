(ns web-scraper.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(def db-spec {:dbtype "sqlite" :dbname "results.db"})

(defn init-table! []
;;  "Создает таблицу scraped_data, если она не существует"
  (jdbc/execute! db-spec
                 ["CREATE TABLE IF NOT EXISTS scraped_data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source TEXT NOT NULL,
                    type TEXT NOT NULL,
                    title TEXT,
                    content TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                  )"])
  (println "[DB] Таблица инициализирована."))

(defn insert-result! [data-map]
 ;; "Вставляет карту данных в таблицу"
  (when data-map
    (try
      ;; КРИТИЧНО: преобразуем ключевое слово :type → строка для SQLite
      (let [filtered {:source (:source data-map)
                      :type (str (:type data-map))  ;; :static → "static"
                      :title (:title data-map)
                      :content (:content data-map)}]
        (sql/insert! db-spec :scraped_data filtered)
        (println (str "[DB] Запись сохранена: " (:source data-map) " (" (:type filtered) ")")))
      (catch Exception e
        (println (str "[DB] Ошибка вставки: " (.getMessage e)))
        (.printStackTrace e)))))

(defn fetch-all-results []
  ;;"Получает все строки из таблицы"
  (try
    (sql/query db-spec ["SELECT * FROM scraped_data ORDER BY id DESC"])
    (catch Exception e
      (println (str "[DB] Ошибка чтения: " (.getMessage e)))
      [])))
