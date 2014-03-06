(ns easy-app.async
  (:require [easy-app.async.go-machine :as machine]
            [easy-app.async.promise :as prom])
  (:import (java.lang Throwable)))

(defn <<
  "Gets a val from promise. Must be called inside a (go ...) block.
  Will park if nothing is available."
  [promise]
  (assert nil "<< used not in (go ...) block"))

(defmacro <? [promise]
  `(let [ret (<< ~promise)]
     (when (instance? Throwable ret)
       (throw ret))
     ret))

(defmacro go [& body]
  `(go* (try
          ~@body
          (catch Throwable ex
            ex))))

(defmacro go* [& body]
  `(let [p# (prom/make)
         captured-bindings# (clojure.lang.Var/getThreadBindingFrame)
         f# ~(machine/make body 1 &env machine/async-custom-terminators)
         state# (-> (f#)
                    (machine/aset-all! machine/USER-START-IDX p#
                                       machine/BINDINGS-IDX captured-bindings#))]
     (machine/run state#)
     p#))
