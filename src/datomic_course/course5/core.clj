(ns datomic-course.course5.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course5.db.config :as db.config]
            [datomic-course.course5.db.produto :as db.produto]
            [datomic-course.course5.db.venda :as db.venda]
            [schema.core :as s]))

(s/set-fn-validation! true)

(db.config/apaga-banco!)
(def conn (db.config/abre-conexao!))
(db.config/cria-schema! conn)
(db.config/cria-dados-de-exemplo! conn)

(def produtos (db.produto/todos (d/db conn)))
(def primeiro (first produtos))
(pprint primeiro)

(def venda1 (db.venda/adiciona! conn (:produto/id primeiro) 3))
(def venda2 (db.venda/adiciona! conn (:produto/id primeiro) 4))
(pprint venda1)

(pprint (db.venda/custo (d/db conn) venda1))
(pprint (db.venda/custo (d/db conn) venda2))

(pprint @(db.produto/adiciona-ou-altera! conn [{:produto/id    (:produto/id primeiro)
                                                :produto/preco 300M}]))


(pprint @(db.venda/cancela! conn venda1))

(pprint (count (db.venda/todas-nao-canceladas (d/db conn))))

(pprint (count (db.venda/todas-inclusive-canceladas (d/db conn))))
(pprint (count (db.venda/canceladas (d/db conn))))



(pprint @(db.produto/adiciona-ou-altera! conn [{:produto/id    (:produto/id primeiro)
                                                :produto/preco 300M}]))
(pprint @(db.produto/adiciona-ou-altera! conn [{:produto/id    (:produto/id primeiro)
                                                :produto/preco 250M}]))
(pprint @(db.produto/adiciona-ou-altera! conn [{:produto/id    (:produto/id primeiro)
                                                :produto/preco 277M}]))
(pprint @(db.produto/adiciona-ou-altera! conn [{:produto/id    (:produto/id primeiro)
                                                :produto/preco 21M}]))

(pprint (db.produto/historico-de-precos (d/db conn) (:produto/id primeiro)))



(pprint @(db.venda/altera-situacao! conn venda1 "preparando"))
(pprint @(db.venda/altera-situacao! conn venda2 "preparando"))
(pprint @(db.venda/altera-situacao! conn venda2 "a caminho"))
(pprint @(db.venda/altera-situacao! conn venda2 "entregue"))

(pprint (db.venda/historico (d/db conn) venda2))

(pprint @(db.venda/cancela! conn venda1))
(pprint (db.venda/historico (d/db conn) venda1))


(pprint (count (db.venda/todas-nao-canceladas (d/db conn))))
(pprint (count (db.venda/todas-inclusive-canceladas (d/db conn))))
(pprint (count (db.venda/canceladas (d/db conn))))

(pprint (db.venda/historico-geral (d/db conn) #inst "2011-10-09T15:00:59.443-00:00"))

(pprint (db.venda/historico-geral (d/db conn) #inst "2019-10-09T15:22:27.518-00:00"))

;86758976 :venda/id 1 15 true
;86758976 :venda/situacao "nova" 15 true
;86758976 :venda/situacao "nova" 20 false
;86758976 :venda/situacao "cancelada" 20 true
;17
;:venda/id 1
