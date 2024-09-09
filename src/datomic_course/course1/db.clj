  (ns datomic-course.course1.db
    (:use clojure.pprint)
    (:require [datomic.api :as d]))

  (def db-uri "datomic:dev://localhost:4334/ecommerce")

  (defn abre-conexao []
    (d/create-database db-uri)
    (d/connect db-uri))

  (defn apaga-banco []
    (d/delete-database db-uri))


  (def schema [{:db/ident       :produto/nome
                :db/valueType   :db.type/string
                :db/cardinality :db.cardinality/one
                :db/doc         "O nome de um produto"}
               {:db/ident       :produto/slug
                :db/valueType   :db.type/string
                :db/cardinality :db.cardinality/one
                :db/doc         "O caminho para acessar esse produto via http"}
               {:db/ident       :produto/preco
                :db/valueType   :db.type/bigdec
                :db/cardinality :db.cardinality/one
                :db/doc         "O preco de um produto com precisão monetária"}
               {:db/ident       :produto/palavra-chave
                :db/ValueType   :db.type/string
                :db/cardinality :db.cardinality/many}
               ])

  (defn cria-schema [conn]
    (d/transact conn schema))

  ; This pull query is used to get the values of the attributes of the entity
  ; it can be [*] to get all attributes
  (defn todos-os-produtos [db]
    (d/q '[:find (pull ?entidade [:produto/nome :produto/slug :produto/preco])
           :where [?entidade :produto/nome ?nome]]
         db))

  (defn todos-os-produtos-por-slug [db slug]
    (d/q '[:find ?entidade
           :in $ ?slug-a-ser-buscado
           :where [?entidade :produto/slug ?slug-a-ser-buscado]] db slug))

  (defn todos-os-slugs [db]
    (d/q '[:find ?qualquer-valor
           :where [?entidade :produto/slug ?qualquer-valor]] db))

  ; Those keys are used to name specific values in the query
  (defn todos-os-produtos-por-preco [db]
    (d/q '[:find ?nome, ?preco
           :keys produto/nome, produto/preco
           :where [?produto :produto/preco ?preco]
                  [?produto :produto/nome ?nome]] db))

  ; Query with minimum value
  (defn todos-os-produtos-por-preco [db preco-minimo-requisitado]
    (d/q '[:find ?nome, ?preco
           :in $, ?preco-minimo
           :keys produto/nome, produto/preco
           :where [?produto :produto/preco ?preco]
                  [?produto :produto/nome ?nome]
                  [(> ?preco ?preco-minimo)]]
         db, preco-minimo-requisitado))


(defn todos-os-produtos-por-palavra-chave[db palavra-chave-buscada]
  (d/q '[:find(pull ?produto[*])
         :in $ ?palavra-chave`
         :were [?produto :produto/palavra-chave ?palavra-chave]]
       db palavra-chave-buscada))