(ns dar.async.promise
  (:import (java.util.concurrent.atomic AtomicBoolean)))

(set! *warn-on-reflection* true)

(defprotocol IPromise
  (abort! [this]
    "Notifies the underlying async computation that the
    result is no longer needed. Generally it should
    release all resources and yield an exception as a promised value.
    However, this method is advisory. Computation might still
    complete successfully after abort. This method might be called
    several times.")
  (deliver! [this val]
    "Delivers the supplied value to promise and notifies
    all registered `then` callbacks. Subequent calls
    will be ignored.")
  (delivered? [this]
    "Returns true if the result was already delivered.")
  (then [this cb]
    "Registers a result handler function. It might be called
    immedeately if the promise was already delivered.")
  (value [this]
    "Returns the delivered value or nil"))

(extend-protocol IPromise
  nil
  (abort! [_])
  (deliver! [_ _] nil)
  (delivered? [_] true)
  (then [_ cb] (cb nil))
  (value [_] nil)

  Object
  (abort! [_])
  (deliver! [_ _] nil)
  (delivered? [_] true)
  (then [this cb] (cb this))
  (value [this] this))

(defrecord PromiseState [val has-value? callbacks])

(deftype Promise [state abort-cb ^AtomicBoolean aborted?]
  IPromise
  (deliver! [this val] (let [next-state (swap! state (fn [state]
                                                       (if (:has-value? state)
                                                         (assoc state :callbacks nil)
                                                         (assoc state :val val :has-value? true))))
                             callbacks (:callbacks next-state)]
                         (when callbacks
                           (swap! state assoc :callbacks nil)
                           (doseq [cb! callbacks]
                             (cb! val)))
                         nil))

  (delivered? [this] (:has-value? @state))

  (then [this cb] (let [state* (swap! state #(if (:has-value? %)
                                               %
                                               (update-in % [:callbacks] conj cb)))]
                    (when (:has-value? state*)
                      (cb (:val state*)))))

  (value [this] (:val @state))

  (abort! [this] (when abort-cb
                   (when (.compareAndSet aborted? false true)
                     (abort-cb this)))))

(defn new-promise
  ([] (new-promise nil))
  ([abort-cb]
   (Promise.
     (atom (->PromiseState nil false nil))
     abort-cb
     (AtomicBoolean. false))))
