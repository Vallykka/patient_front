(ns patient-front.validator
  (:require [clojure.string :refer [blank?]]))

(defn invalid?
  [patient]
  (map :label
    (filter #(blank? (:value %))
      (filter :required
        (filter map?
          (tree-seq map? vals patient))))))
