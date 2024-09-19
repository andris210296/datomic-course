(ns datomic-course.course3.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course3.db :as db]
            [datomic-course.course3.model :as model]
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

; This will fix the "race condition" that was happening in the previous code
; when you want to change different attributes of the same entity
(defn atualiza-preco-inteligente []
  (println "atualizando preco")
  (let [produto {:produto/id (:produto/id dama) :produto/preco 111M}]
    (db/adiciona-ou-altera-produtos! conn [produto])
    (println "Atualizando preco")
    produto))

(defn atualiza-slug-inteligente []
  (println "atualizando slug")
  (let [produto {:produto/id (:produto/id dama) :produto/slug "/dama-com-slug-novo"}]
    (Thread/sleep 3000)
    (db/adiciona-ou-altera-produtos! conn [produto])
    (println "Atualizando preco")
    produto))


(def produtos (db/todos-os-produtos (d/db conn)))
(def primeiro-produto (first produtos))

(pprint (db/um-produto (d/db conn) (:produto/id primeiro-produto)))

; If it doesn't exist in database the return will be nil
(pprint (db/um-produto (d/db conn)  (model/uuid) ))

; If it doesn't exist in database the return will be an exception
(pprint (db/um-produto! (d/db conn)  (model/uuid) ))

(pprint (db/todos-os-produtos-vendaveis (d/db conn)))

(pprint (db/um-produto-vendavel! (d/db conn) (:produto/id primeiro-produto)))

(pprint (db/todos-os-produtos-vendaveis-rules (d/db conn)))

(pprint (db/um-produto-vendavel-rules! (d/db conn) (:produto/id primeiro-produto)))