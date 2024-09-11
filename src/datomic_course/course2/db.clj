  (ns datomic-course.course2.db
    (:use clojure.pprint)
    (:require [datomic.api :as d]))

  (def db-uri "datomic:dev://localhost:4334/ecommerce")

  (defn abre-conexao []
    (d/create-database db-uri)
    (d/connect db-uri))

  (defn apaga-banco! []
    (d/delete-database db-uri))


(def schema [
             ; Produtos
             {:db/ident       :produto/nome
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
             {:db/ident       :produto/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident        :produto/categoria
              :db/valueType    :db.type/ref
              :db/cardinality  :db.cardinality/one}

             ; Categorias
             {:db/ident         :categoria/nome
              :db/valueType     :db.type/string
              :db/cardinality   :db.cardinality/one }
             {:db/ident         :categoria/id
              :db/valueType     :db.type/uuid
              :db/cardinality   :db.cardinality/one
              :db/unique        :db.unique/identity}

             ; Transações
             {:db/ident       :tx-data/ip
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             ])

  (defn cria-schema! [conn]
    (d/transact conn schema))

  (defn todos-os-produtos [db]
    (d/q '[:find (pull ?entidade [*])
           :where [?entidade :produto/nome ?nome]]
         db))

  (defn um-produto-por-dbid [db db-id]
  (d/pull db '[*] db-id))


(defn um-produto[db produto-id]
  (d/pull db '[*] [:produto/id produto-id]))


(defn todas-as-categorias [db]
  (d/q '[:find (pull ?categoria [*])
         :where [?categoria :categoria/id]]
       db))


(defn db-adds-de-atribuicao-de-categorias [produtos categoria]
  (reduce (fn [db-adds produto] (conj db-adds [:db/add
                                               [:produto/id (:produto/id produto)]
                                               :produto/categoria
                                               [:categoria/id (:categoria/id categoria)]]))
          []
          produtos))

(defn atribui-categorias! [conn produtos categoria]
  (let [a-transacionar (db-adds-de-atribuicao-de-categorias produtos categoria)]
    (d/transact conn a-transacionar)))

  ; This code adds a product and more information on the transaction
(defn adiciona-produtos!
  ([conn produtos]
   (d/transact conn produtos))
  ([conn produtos ip]
   (let [db-add-ip [:db/add "datomic.tx" :tx-data/ip ip]]
     (d/transact conn (conj produtos db-add-ip)))))

(defn adiciona-categorias! [conn categorias]
  (d/transact conn categorias))

(defn todos-os-nomes-de-produtos-e-categorias [db]
  (d/q '[:find ?nome-do-produto ?nome-da-categoria
         :keys produto categoria
         :where [?produto :produto/nome ?nome-do-produto]
                [?produto :produto/categoria ?categoria]
                [?categoria :categoria/nome ?nome-da-categoria]]
       db))

; exemplo com forward navigation
; ?produto :produto/categoria >>>>>
;(defn todos-os-produtos-da-categoria [db nome-da-categoria]
;  (d/q '[:find (pull ?produto [:produto/nome :produto/slug {:produto/categoria [:categoria/nome]}])
;         :in $ ?nome
;         :where [?categoria :categoria/nome ?nome]
;                [?produto :produto/categoria ?categoria]]
;       db nome-da-categoria))

; exemplo com backward navigation
; >>>>>>>>>> :produto/_categoria ?categoria
(defn todos-os-produtos-da-categoria [db nome-da-categoria]
  (d/q '[:find (pull ?categoria [:categoria/nome {:produto/_categoria [:produto/nome :produto/slug]}])
         :in $ ?nome
         :where [?categoria :categoria/nome ?nome]]
       db nome-da-categoria))


;#{2500.10M, 888888.10M, 0.1M, 30M, 15M}
;#{[423789978243768 2500.10M], [132123 888888.10M], [1312 0.1M], [122134 30M], [4323 15M], [7864323489 30M]}
(defn resumo-dos-produtos [db]
  (d/q '[:find (min ?preco) (max ?preco) (count ?preco) (sum ?preco)
         :keys minimo maximo quantidade preco-total
         :with ?produto
         :where [?produto :produto/preco ?preco]]
       db))

(defn resumo-dos-produtos-por-categoria [db]
  (d/q '[:find ?nome (min ?preco) (max ?preco) (count ?preco) (sum ?preco)
         :keys categoria minimo maximo quantidade preco-total
         :with ?produto
         :where [?produto :produto/preco ?preco]
         [?produto :produto/categoria ?categoria]
         [?categoria :categoria/nome ?nome]]
       db))


; two queries connected
;(defn todos-os-produtos-mais-caros [db]
;  (let [preco-mais-alto (ffirst (d/q '[:find (max ?preco)
;                                       :where [_ :produto/preco ?preco]]
;                                     db))]
;    (d/q '[:find (pull ?produto [*])
;           :in $ ?preco
;           :where [?produto :produto/preco ?preco]]
;         db preco-mais-alto)))

; Better approach with one query
(defn todos-os-produtos-mais-caros [db]
  (d/q '[:find (pull ?produto [*])
         :where [(q '[:find (max ?preco)
                      :where [_ :produto/preco ?preco]]
                    $) [[?preco]]]
         [?produto :produto/preco ?preco]]
       db))

(defn todos-os-produtos-mais-baratos [db]
  (d/q '[:find (pull ?produto [*])
         :where [(q '[:find (min ?preco)
                      :where [_ :produto/preco ?preco]]
                    $) [[?preco]]]
         [?produto :produto/preco ?preco]]
       db))

(defn todos-os-produtos-do-ip [db ip]
  (d/q '[:find (pull ?produto [*])
         :in $ ?ip-buscado
         :where [?transacao :tx-data/ip ?ip-buscado]
         [?produto :produto/id _ ?transacao]]
       db ip))