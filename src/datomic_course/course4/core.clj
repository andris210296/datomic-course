(ns datomic-course.course4.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course4.db :as db]
            [datomic-course.course4.model :as model]
            [schema.core :as s]))
(s/set-fn-validation! true)


; Reset the database and adds new values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(db/apaga-banco!)
(def conn (db/abre-conexao))
(db/cria-schema! conn)

(db/cria-dados-de-exemplo conn)


(pprint (db/todos-os-produtos(d/db conn)))
(pprint (db/todas-as-categorias (d/db conn)))


(def dama {
           :produto/nome "Dama"
            :produto/slug "/dama"
            :produto/preco 15M
           :produto/id (model/uuid)
           })
(db/adiciona-ou-altera-produtos! conn [dama])
(pprint (db/um-produto (d/db conn) (:produto/id dama)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pprint (db/todos-os-produtos-nas-categorias (d/db conn) ["Eletrônicos" "Alimentação"]))
(pprint (db/todos-os-produtos-nas-categorias (d/db conn) ["Eletrônicos" "Esporte"]))
(pprint (db/todos-os-produtos-nas-categorias (d/db conn) ["Esporte"]))
(pprint (db/todos-os-produtos-nas-categorias (d/db conn) []))
(pprint (db/todos-os-produtos-nas-categorias (d/db conn) ["Alimentação"]))

(pprint (db/todos-os-produtos-nas-categorias-e-digital (d/db conn) ["Esporte"] true))
(pprint (db/todos-os-produtos-nas-categorias-e-digital (d/db conn) ["Esporte"] false))
(pprint (db/todos-os-produtos-nas-categorias-e-digital (d/db conn) ["Eletrônicos"] true))



(def produtos (db/todos-os-produtos (d/db conn)))
(def primeiro (first produtos))
(pprint primeiro)

; Make sure that the old value is correct in order to be updated
(pprint @(db/atualiza-preco! conn (:produto/id primeiro) 2500.10M 30M))
(pprint @(db/atualiza-preco! conn (:produto/id primeiro) 30M 35M))
;(pprint @(db/atualiza-preco! conn (:produto/id primeiro) 30M 45M))


; perdemos validação de schema
;(db/atualiza-produto conn [:produto/preco 20M 30M ] [:produto/slug :a :b])

(def segundo (second produtos))
(pprint segundo)
(def a-atualizar { :produto/id (:produto/id segundo) :produto/preco 3000M, :produto/estoque 8 })

(pprint @(db/atualiza-produto! conn segundo a-atualizar))

; tenta novamente, mas não deve funcionar!!!
(pprint @(db/atualiza-produto! conn segundo a-atualizar))


(pprint @(db/adiciona-variacao! conn (:produto/id primeiro) "Season pass" 40M))
(pprint @(db/adiciona-variacao! conn (:produto/id primeiro) "Season pass 4 anos" 60M))

(pprint (d/q '[:find (pull ?produto [*])
               :where [?produto :produto/nome]]
             (d/db conn)))

(pprint (db/todos-os-produtos (d/db conn)))

(pprint (db/total-de-produtos (d/db conn)))
(pprint @(db/remove-produto! conn (:produto/id primeiro)))
(pprint (db/total-de-produtos (d/db conn)))

(pprint (d/q '[:find ?nome
               :where [_ :variacao/nome ?nome]]
             (d/db conn)))




(def produtos (db/todos-os-produtos (d/db conn)))
(def primeiro (first produtos))
(pprint primeiro)

(dotimes [n 10] (db/visualizacao! conn (:produto/id primeiro)))
(pprint (db/um-produto (d/db conn) (:produto/id primeiro)))
