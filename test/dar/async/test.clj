(ns dar.async.test
  (:require [clojure.test :refer :all]
            [dar.async :refer :all]
            [dar.async.promise :refer :all]))

(deftest abort-propagation
  (let [p (new-promise (fn [this]
                         (deliver! this "aborted")))
        res (go (<< p))]
    (is (= false (delivered? p)))
    (abort! p)
    (is (= "aborted" (value p)))))
