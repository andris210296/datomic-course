(ns datomic-course.course4.db
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course4.model :as model]
            [schema.core :as s]
            [clojure.walk :as walk]
            [clojure.set :as cset]))


(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn abre-conexao []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn apaga-banco! []
  (d/delete-database db-uri))


(def schema [

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
              :db/doc         "O preço de um produto com precisão monetária"}
             {:db/ident       :produto/palavra-chave
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many}
             {:db/ident       :produto/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :produto/categoria
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident       :produto/estoque
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident       :produto/digital
              :db/valueType   :db.type/boolean
              :db/cardinality :db.cardinality/one}
             {:db/ident       :produto/variacao
              :db/valueType   :db.type/ref
              :db/isComponent true
              :db/cardinality :db.cardinality/many}
             {:db/ident       :produto/visualizacoes
              :db/valueType   :db.type/long
              :db/cardinality :db.cardinality/one
              :db/noHistory   true}

             {:db/ident       :variacao/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :variacao/nome
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :variacao/preco
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one}

             {:db/ident       :categoria/nome
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :categoria/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}


             {:db/ident       :tx-data/ip
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}

             ])


(s/defn adiciona-ou-altera-produtos!
  ([conn produtos :- [model/Produto]]
   (d/transact conn produtos))
  ([conn produtos :- [model/Produto] ip]
   (let [db-add-ip [:db/add "datomic.tx" :tx-data/ip ip]]
     (d/transact conn (conj produtos db-add-ip)))))

(defn cria-schema! [conn]
  (d/transact conn schema))

(defn dissoc-db-id [entidade]
  (if (map? entidade)
    (dissoc entidade :db/id)
    entidade))

; Example using walk prewalk
(defn datomic-para-entidade [entidades]
  (walk/prewalk dissoc-db-id entidades))

; The maybe allows us to return nil if the entity is not found and also forcing the return to be a Produto
; Must be carefully with NullPointerException
(s/defn um-produto :- (s/maybe model/Produto) [db produto-id :- java.util.UUID]
  (let [resultado (d/pull db '[* {:produto/categoria [*]}] [:produto/id produto-id])
        produto (datomic-para-entidade resultado)]
    (if(:produto/id produto)
      produto
      nil)))

(s/defn um-produto! :- model/Produto [db produto-id :- java.util.UUID]
  (let [produto (um-produto db produto-id)]
    (when (nil? produto)
      (throw (ex-info "Produto não encontrado" {:type :errors/not-found :id produto-id}))
      produto)))

(s/defn todos-os-produtos :- [model/Produto] [db]
  (datomic-para-entidade
    (d/q '[:find [(pull ?entidade [* {:produto/categoria [*]}]) ...]
         :where [?entidade :produto/nome]]
         db)))

(s/defn todas-as-categorias :- [model/Categoria] [db]
 (datomic-para-entidade
   (d/q '[:find [(pull ?categoria [*]) ...]
         :where [?categoria :categoria/id]]
       db)))


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

(s/defn adiciona-categorias! [conn categorias :- [model/Categoria]]
  (d/transact conn categorias))

(s/defn todos-os-produtos-vendaveis :- [model/Produto] [db]
  (datomic-para-entidade
    (d/q '[:find [(pull ?produto [* {:produto/categoria [*]}]) ...]
           :in $ %
           :where [?produto :produto/nome]
           [?produto :produto/estoque ?estoque]
           [(> ?estoque 0)]]
         db)))

(s/defn um-produto-vendavel! :- model/Produto [db produto-id :- java.util.UUID]
  (let [query '[:find (pull ?produto [* {:produto/categoria [*]}]) .
                :in $ ?id
                :where [?produto :produto/id ?id]
                [?produto :produto/estoque ?estoque]
                [(> ?estoque 0)]]
        resultado (d/q query db produto-id)
        produto (datomic-para-entidade resultado)]
    (if (:produto/id produto)
      produto
      nil)))


(def regras
  '[
    [(estoque ?produto ?estoque)
     [?produto :produto/estoque ?estoque]]
    [(estoque ?produto ?estoque)
     [?produto :produto/digital true]
     [(ground 100) ?estoque]]
    [(pode-vender? ?produto)
     (estoque ?produto ?estoque)
     [(> ?estoque 0)]]
    [(produto-na-categoria? ?produto ?nome-da-categoria)
     [?categoria :categoria/nome ?nome-da-categoria]
     [?produto :produto/categoria ?categoria]]
    ])


(s/defn todos-os-produtos-vendaveis-rules :- [model/Produto] [db]
  (datomic-para-entidade
    (d/q '[:find [(pull ?produto [* {:produto/categoria [*]}]) ...]
           :in $ %
           :where (pode-vender? ?produto)]
         db regras)))

(s/defn um-produto-vendavel-rules! :- model/Produto [db produto-id :- java.util.UUID]
  (let [query '[:find (pull ?produto [* {:produto/categoria [*]}]) .
                :in $ % ?id
                :where [?produto :produto/id ?id]
                (estoque ?produto ?estoque)
                (pode-vender? ?produto)]
        resultado (d/q query db regras produto-id)
        produto (datomic-para-entidade resultado)]
    (if (:produto/id produto)
      produto
      nil)))




(defn cria-dados-de-exemplo [conn]

  (def eletronicos (model/nova-categoria "Eletrônicos"))
  (def esporte (model/nova-categoria "Esporte"))
  (pprint @(adiciona-categorias! conn [eletronicos, esporte]))


  (def computador (model/novo-produto (model/uuid) "Computador Novo", "/computador-novo", 2500.10M, 10))
  (def celular (model/novo-produto (model/uuid) "Celular Caro", "/celular", 888888.10M))
  ;(def calculadora {:produto/nome "Calculadora com 4 operações"})
  (def celular-barato (model/novo-produto "Celular Barato", "/celular-barato", 0.1M))
  (def xadrez (model/novo-produto (model/uuid) "Tabuleiro de xadrez", "/tabuleiro-de-xadrez", 30M, 5))
  (def jogo (assoc (model/novo-produto (model/uuid) "Jogo online", "/jogo-online", 20M) :produto/digital true))

  (pprint @(adiciona-ou-altera-produtos! conn [computador, celular, celular-barato, xadrez, jogo] "200.216.222.125"))

  ; This code will connect a product to a category

  (atribui-categorias! conn [computador, celular, celular-barato, jogo] eletronicos)
  (atribui-categorias! conn [xadrez] esporte)

  )

(s/defn todos-os-produtos-nas-categorias :- [model/Produto] [db categorias :- [s/Str]]
  (datomic-para-entidade
    (d/q '[:find [(pull ?produto [* {:produto/categoria [*]}]) ...]
           :in $ % [?nome-da-categoria ...]
           :where
           (produto-na-categoria? ?produto ?nome-da-categoria)]
         db regras categorias)))

(s/defn todos-os-produtos-nas-categorias-e-digital :- [model/Produto] [db categorias :- [s/Str] digital? :- s/Bool]
  (datomic-para-entidade
    (d/q '[:find [(pull ?produto [* {:produto/categoria [*]}]) ...]
           :in $ % [?nome-da-categoria ...] ?eh-digital?
           :where
           (produto-na-categoria? ?produto ?nome-da-categoria)
           [?produto :produto/digital ?eh-digital?]
           ]
         db regras categorias digital?)))

;
;(s/defn atualiza-preco
;  [conn, produto-id :- java.util.UUID, preco-antigo :- BigDecimal, preco-novo :- BigDecimal]
;  (if (= preco-antigo (:produto/preco (d/pull conn [*] (:produto/id produto-id))))
;    ; essa abordagem horrível não garante atomicidade
;    ; a linha anterior rodou. ponto. a linha seguinte roda.
;    ; não garante o que queríamos :(
;    (d/transact conn [{:produto/id produto-id :produto/preco preco-novo}])
;    (throw (ex-info "Valor foi alterado entre leitura e escrita" {:type :errors/transaction-validation-error}))))


(s/defn atualiza-preco!
  [conn, produto-id :- java.util.UUID, preco-antigo :- BigDecimal, preco-novo :- BigDecimal]
  (d/transact conn [[:db/cas [:produto/id produto-id] :produto/preco preco-antigo preco-novo]]))


; this code joins the old and new attributes and then creates a transaction
(s/defn atualiza-produto!
  [conn, antigo :- model/Produto, a-atualizar :- model/Produto]
  (let [produto-id (:produto/id antigo)
        atributos (cset/intersection (set (keys antigo)) (set (keys a-atualizar)))
        atributos (disj atributos :produto/id)
        txs (map (fn [atributo] [:db/cas [:produto/id produto-id] atributo (get antigo atributo) (get a-atualizar atributo)]) atributos)]
    (d/transact conn txs)))

(s/defn adiciona-variacao!
  [conn produto-id :- java.util.UUID variacao :- s/Str preco :- BigDecimal]
  (d/transact conn [{:db/id          "variacao-temporaria"
                     :variacao/nome  variacao
                     :variacao/preco preco
                     :variacao/id    (model/uuid)}
                    {:produto/id       produto-id
                     :produto/variacao "variacao-temporaria"}]))

(defn total-de-produtos [db]
  (d/q '[:find [(count ?produto)]
         :where [?produto :produto/nome]]
       db))

; removes also an entity
(s/defn remove-produto!
  [conn produto-id :- java.util.UUID]
  (d/transact conn [[:db/retractEntity [:produto/id produto-id]]]))

(s/defn visualizacoes [db, produto-id :- java.util.UUID]
  (or (d/q '[:find ?visualizacoes .
             :in $ ?id
             :where [?p :produto/id ?id]
             [?p :produto/visualizacoes ?visualizacoes]]
           db produto-id) 0))
; Whe are doing two calls to DB, which does not guarantee atomicity
(s/defn visualizacao! [conn produto-id :- java.util.UUID]
  (let [ate-agora (visualizacoes (d/db conn) produto-id)
        novo-valor (inc ate-agora)]
    (d/transact conn [{:produto/id            produto-id
                       :produto/visualizacoes novo-valor}])))


; this fix the problem of the previous function
(def incrementa-visualizacao
  #db/fn {
          :lang :clojure
          :params [db produto-id]
          :code (let [visualizacoes (d/q '[:find ?visualizacoes .
                                           :in $ ?id
                                           :where [?produto :produto/id ?id]
                                           [?produto :produto/visualizacoes ?visualizacoes]]
                                         db produto-id)
                      atual (or visualizacoes 0)
                      total-novo (inc atual)]
                  [{:produto/id produto-id
                    :produto/visualizacoes total-novo}])})

(pprint @(d/transact
          conn
          [{:db/doc "Incrementa :produto/visualizacoes"
            :db/ident :increment-visualizacao
            :db/fn incrementa-visualizacao}]))
