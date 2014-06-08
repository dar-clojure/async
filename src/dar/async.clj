(ns dar.async
  (:require [dar.async.go-machine :as machine]
            [dar.async.promise :refer :all]))

(defn <<
  "Gets a val from promise. Must be called inside a (go ...) block.
  Will 'block' and wait for result if it is not available."
  [promise]
  (assert nil "<< used not in (go ...) block"))

(defmacro go* [& body]
  `(machine/go ~@body))

(defmacro go
  "Executes the body synchronously (like a (do ...) block) until it
  hits the first async point (call to either << or <?).
  If the result is immediately available it continues running
  in the current thread. Otherwise the execution will be paused and
  resumed on a thread that delivered result."
  [& body]
  `(go* (try
          ~@body
          (catch Throwable ex#
            ex#))))

(defmacro <?
  "Like <<, but it will rethrow the result value if it is throwable."
  [p]
  `(let [ret# (<< ~p)]
     (when (instance? Throwable ret#)
       (throw ret#))
     ret#))

(defmacro <<!
  "Gets a value from promise. Will block until result is delivered."
  [p]
  `(let [native-promise# (promise)]
     (then ~p #(deliver native-promise# %))
     @native-promise#))

(defmacro <?! [p]
  "Like <<!, but it will rethrow the result value if it is throwable."
  `(let [ret# (<<! ~p)]
     (when (instance? Throwable ret#)
       (throw ret#))
     ret#))
