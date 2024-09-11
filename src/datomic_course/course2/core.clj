(ns datomic-course.course2.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course2.db :as db]
            [datomic-course.course2.model :as model]))



; Reset the database and adds new values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(db/apaga-banco!)
(def conn (db/abre-conexao))
(db/cria-schema! conn)


(def eletronicos (model/nova-categoria "Eletrônicos"))
(def esporte (model/nova-categoria "Esporte"))

(pprint @(db/adiciona-categorias! conn [eletronicos, esporte]))

(def categorias (db/todas-as-categorias (d/db conn)))
(pprint categorias)


(def computador (model/novo-produto (model/uuid) "Computador Novo", "/computador-novo", 2500.10M))
(def celular (model/novo-produto (model/uuid) "Celular Caro", "/celular", 888888.10M))
(def calculadora {:produto/nome "Calculadora com 4 operações"})
(def celular-barato (model/novo-produto "Celular Barato", "/celular-barato", 0.1M))
(def xadrez (model/novo-produto "Tabuleiro de xadrez", "/tabuleiro-de-xadrez", 30M))

(pprint @(db/adiciona-produtos! conn [computador, celular, calculadora, celular-barato, xadrez] "200.216.222.125"))

; This code will connect a product to a category

(db/atribui-categorias! conn [computador, celular, celular-barato] eletronicos)
(db/atribui-categorias! conn [xadrez] esporte)


; This code creates a product and a category
(pprint @(db/adiciona-produtos! conn [{:produto/nome      "Camiseta"
                                       :produto/slug      "/camiseta"
                                       :produto/preco     30M
                                       :produto/id        (model/uuid)
                                       :produto/categoria {:categoria/nome "Roupas"
                                                           :categoria/id   (model/uuid)}}]))

; This code creates a product with an existing category
(def esporte-id (:categoria/id esporte))
(pprint @(db/adiciona-produtos! conn [{:produto/nome      "Dama"
                                       :produto/slug      "/dama"
                                       :produto/preco     15M
                                       :produto/id        (model/uuid)
                                       :produto/categoria [:categoria/id esporte-id]}]))

(pprint (db/todos-os-produtos(d/db conn)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;




; Getting a product based on an id
(def produtos (db/todos-os-produtos (d/db conn)))
(def primeiro-db-id (-> produtos
                             ffirst
                             :db/id))
(println "O dbid do primeiro produto é" primeiro-db-id)
(print (db/um-produto-por-dbid (d/db conn) primeiro-db-id))


; Getting a product based on uuid
(def primeiro-produto-id (-> produtos
                             ffirst
                             :produto/id))
(println "O id do primeiro produto é" primeiro-produto-id)
(print (db/um-produto(d/db conn) primeiro-produto-id))


; Getting a product name with its category
(pprint (db/todos-os-nomes-de-produtos-e-categorias (d/db conn)))

; Getting all products from a category
(pprint (db/todos-os-produtos-da-categoria (d/db conn) "Eletrônicos"))


(pprint (db/resumo-dos-produtos (d/db conn)))
(pprint (db/resumo-dos-produtos-por-categoria (d/db conn)))


(pprint (db/todos-os-produtos-mais-caros (d/db conn)))


(pprint (db/todos-os-produtos-mais-baratos (d/db conn)))


(pprint (db/todos-os-produtos-do-ip (d/db conn) "200.216.222.125"))