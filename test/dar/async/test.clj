(ns dar.async.test
  (:require [clojure.test :refer :all]
            [dar.async :refer :all]
            [dar.async.promise :refer :all]))

(deftest promise-test
  (let [p (new-promise)]
    (is (not (delivered? p)))
    (deliver! p 1)
    (is (= 1 (value p)))
    (testing "Ignores subsequent deliverings"
      (deliver! p 2)
      (is (= 1 (value p))))
    (testing "Calls callback immediately"
      (let [called? (atom false)]
        (then p (fn [val]
                  (is (= 1 val))
                  (reset! called? true)))
        (is called?)))))

(deftest abort-propagation
  (let [p (new-promise (fn [this]
                         (deliver! this "aborted")))
        res (go (<< p))]
    (is (not (delivered? p)))
    (abort! p)
    (is (= "aborted" (value p)))))

(deftest basic
  (is (= 10 (value (go
                     (+ (<? 5) (<? 5)))))))

(deftest try-catch
  (let [entered? (atom false)
        called? (atom false)
        res (go
              (try
                (reset! entered? true)
                (<? (ex-info "foo" {:i 10}))
                (reset! called? true)
                (catch Throwable e
                  (-> e ex-data))))]
    (is @entered?)
    (is (not @called?))
    (is (= {:i 10} (value res)))))
