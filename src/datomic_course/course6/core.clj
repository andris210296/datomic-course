(ns datomic-course.course6.core
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [datomic-course.course6.db.config :as db.config]
            [datomic-course.course6.db.produto :as db.produto]
            [datomic-course.course6.model :as model]
            [schema.core :as s]
            [schema-generators.generators :as g]
            [clojure.test.check.generators :as gen]
            [datomic-course.course6.generators :as generators]))

(s/set-fn-validation! true)

(db.config/apaga-banco!)
(def conn (db.config/abre-conexao!))
(db.config/cria-schema! conn)
(db.config/cria-dados-de-exemplo! conn)

(pprint (db.produto/todos (d/db conn)))

(pprint (g/sample 10 model/Categoria))
(pprint (g/sample 10 model/Variacao generators/leaf-generators))

(defn gera-10000-produtos [conn]
  (dotimes [atual 50]
    (def produtos-gerados (g/sample 200 model/Produto generators/leaf-generators))
    (println atual
             (count @(db.produto/adiciona-ou-altera! conn
                                                     produtos-gerados)))))

(println "A geração de produtos")
(time (gera-10000-produtos conn))
(println "Busca do mais caro")
(time (dotimes [_ 100] (db.produto/busca-mais-caro (d/db conn))))
(println "Busca dos mais caros que")
(time (dotimes [_ 100] (count (db.produto/busca-mais-caros-que (d/db conn) 50000M))))

(println "Busca por preco")
(def preco-mais-caro (db.produto/busca-mais-caro (d/db conn)))
(time (dotimes [_ 100] (count (db.produto/busca-por-preco (d/db conn) preco-mais-caro))))

(println "Busca por preco e nome")
(time (dotimes [_ 100] (count (db.produto/busca-por-preco-e-nome (d/db conn) 1000M "com"))))



; Creating my own framework to generate Datomic schema
(defn propriedades-do-valor [valor]
  (if (vector? valor)
    (merge {:db/cardinality :db.cardinality/many}
           (propriedades-do-valor (first valor)))
    (cond (= valor java.util.UUID) {:db/valueType :db.type/uuid
                                    :db/unique    :db.unique/identity}
          (= valor s/Str) {:db/valueType :db.type/string}
          (= valor BigDecimal) {:db/valueType :db.type/bigdec}
          (= valor Long) {:db/valueType :db.type/long}
          (= valor s/Bool) {:db/valueType :db.type/boolean}
          (map? valor) {:db/valueType :db.type/ref}
          :else {:db/valueType (str "desconhecido: " (type valor) valor)})))

(defn extrai-nome-da-chave [chave]
  (cond (keyword? chave) chave
        (instance? schema.core.OptionalKey chave) (get chave :k)
        :else key))

(defn chave-valor-para-definicao [[chave valor]]
  (let [base {:db/ident       (extrai-nome-da-chave chave)
              :db/cardinality :db.cardinality/one}
        extra (propriedades-do-valor valor)
        schema-do-datomic (merge base extra)]
    schema-do-datomic))

(defn schema-to-datomic [definicao]
  (mapv chave-valor-para-definicao definicao))

(pprint (schema-to-datomic model/Categoria))
(pprint (schema-to-datomic model/Variacao))
(pprint (schema-to-datomic model/Venda))
(pprint (schema-to-datomic model/Produto))
