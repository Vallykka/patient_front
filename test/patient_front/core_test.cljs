(ns patient-front.core-test
    (:require
     [cljs.test :refer-macros [deftest is testing]]
     [patient-front.core :refer [multiply]]))

;;todo: write frontend tests
(deftest multiply-test
  (is (= (* 1 2) (multiply 1 2))))

