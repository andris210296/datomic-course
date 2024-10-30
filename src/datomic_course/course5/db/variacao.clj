(ns datomic-course.course5.db.variacao
  (:use clojure.pprint)
  (:require [datomic.api :as d]
            [ecommerce.model :as model]
            [datomic-course.course5.db.entidade :as db.entidade]
            [schema.core :as s]
            [clojure.walk :as walk]
            [clojure.set :as cset]))

(s/defn adiciona-variacao!
  [conn produto-id :- java.util.UUID variacao :- s/Str preco :- BigDecimal]
  (d/transact conn [{:db/id          "variacao-temporaria"
                     :variacao/nome  variacao
                     :variacao/preco preco
                     :variacao/id    (model/uuid)}
                    {:produto/id       produto-id
                     :produto/variacao "variacao-temporaria"}]))
