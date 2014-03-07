(ns dar.async
  (:require [dar.async.go-machine :as machine]
            [dar.async.promise :refer :all])
  (:import (java.lang Throwable)))

(defn <!
  "Gets a val from promise. Must be called inside a (go ...) block.
  Will park if nothing is available."
  [promise]
  (assert nil "<! used not in (go ...) block"))

(defmacro go* [& body]
  `(let [p# (make-promise)
         captured-bindings# (clojure.lang.Var/getThreadBindingFrame)
         f# ~(machine/make body 1 &env machine/async-custom-terminators)
         state# (-> (f#)
                    (machine/aset-all! machine/USER-START-IDX p#
                                       machine/BINDINGS-IDX captured-bindings#))]
     (machine/run state#)
     p#))

(defmacro go [& body]
  `(go* (try
          ~@body
          (catch Throwable ex
            ex))))

(defmacro <? [promise]
  `(let [ret# (<! ~promise)]
     (when (instance? Throwable ret#)
       (throw ret#))
     ret#))

(defmacro <!! [p]
  `(let [native-promise# (promise)]
     (then ~p #(deliver native-promise# %))
     @native-promise#))

(defmacro <?! [promise]
  `(let [ret# (<!! ~promise)]
     (when (instance? Throwable ret#)
       (throw ret#))
     ret#))
