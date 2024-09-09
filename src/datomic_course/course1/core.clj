(ns datomic-course.course1.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course1.db :as db]
            [datomic-course.course1.model :as model]))

(def conn (db/abre-conexao))

; the structure #datom [id-da-entidade atributo valor id-da-tx added?]

; In order to work this transaction to the db, the schema must be created first
(let [computador (model/novo-produto "Computador Novo", "/computador_novo", 2500.10M)]
  (d/transact conn [computador]))

; the snapshot of the db is taken at the time of the transaction
; every time you run this code, the db will be different
(def db (d/db conn))

(d/q '[:find ?entidade ?nome
       :where [?entidade :produto/nome ?nome]]
     db)

(let [celular (model/novo-produto "Celular Caro", "/celular", 888888.10M)]
  (d/transact conn [celular]))

; A new snapshot of the db
(def db (d/db conn))

; now both products are in the db
(d/q '[:find ?entidade ?nome
       :where [?entidade :produto/nome ?nome]]
     db)



; This code creates a new product and then changes its price.
; When updating, it deletes the old value and adds the new one.
; It creates a new entity with the new value without deleting the old one, allowing you to see the old transactions.
; This database is immutable, nad you can check the logs latter

(let [celular-barato (model/novo-produto "Celular Barato" "/celular-barato" 8888.10M)
      resultado @(d/transact conn [celular-barato])
      id-entidade (-> resultado :tempids vals first)]
  (pprint resultado)

  ; This is the "update"
  (pprint @(d/transact conn [[:db/add id-entidade :produto/preco 0.1M]]))

  ; This is how you remove an attribute
  (pprint @(d/transact conn [[:db/retract id-entidade :produto/slug "/celular-barato"]])))



; Adding more than one product at once, if one fails all fail (Atomicity)
(let [computador (model/novo-produto "Computador Novo", "/computador-novo", 2500.10M)
      celular (model/novo-produto "Celular Caro", "/celular", 888888.10M)
      calculadora {:produto/nome "Calculadora com 4 operações"}
      celular-barato (model/novo-produto "Celular Barato", "/celular-barato", 0.1M)]
  (d/transact conn [computador, celular, calculadora, celular-barato]))

(pprint (db/todos-os-produtos(d/db conn)))






; Reset the database and adding new values
(db/apaga-banco)
(def conn (db/abre-conexao))
(db/cria-schema conn)
(let [computador (model/novo-produto "Computador Novo", "/computador-novo", 2500.10M)
      celular (model/novo-produto "Celular Caro", "/celular", 888888.10M)
      calculadora {:produto/nome "Calculadora com 4 operações"}
      celular-barato (model/novo-produto "Celular Barato", "/celular-barato", 0.1M)]
  (d/transact conn [computador, celular, calculadora, celular-barato]))


(pprint (db/todos-os-produtos-por-slug (d/db conn) "/computador-novo"))
(pprint (db/todos-os-slugs (d/db conn)))
(pprint (db/todos-os-produtos-por-preco (d/db conn) 1))




; Getting a filtered query with data from the past
(pprint (db/todos-os-produtos(d/as-of (d/db conn) #inst "2024-02-22")))



; Adding a new value in a many cardinality attribute
(d/transact conn [[:db/add 17592186045418 :produto/palavra-chave "desktop"]
                  [:db/add 17592186045418 :produto/palavra-chave "computador"]])

; Removing an attribute
(d/transact conn [[:db/retract 17592186045418 :produto/palavra-chave "computador"]])