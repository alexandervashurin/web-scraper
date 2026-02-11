(ns web-scraper.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(def db-spec {:dbtype "sqlite" :dbname "results.db"})

(defn init-table! []
  ;;"Создает таблицу scraped_data, если она не существует"
  ;; В next.jdbc используем jdbc/execute! с обычной SQL строкой
  (jdbc/execute! db-spec
                 ["CREATE TABLE IF NOT EXISTS scraped_data (
       id INTEGER PRIMARY KEY AUTOINCREMENT,
       source TEXT,
       type TEXT,
       title TEXT,
       content TEXT,
       created_at DATETIME DEFAULT CURRENT_TIMESTAMP
     )"])
  (println "[DB] Таблица инициализирована."))

(defn insert-result! [data-map]
  ;"Вставляет карту данных в таблицу"
  (when data-map
    (sql/insert! db-spec :scraped_data data-map)))

(defn fetch-all-results []
  ;:"Получает все строки из таблицы"
  (sql/query db-spec ["SELECT * FROM scraped_data ORDER BY id DESC"]))
