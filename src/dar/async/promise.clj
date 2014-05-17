(ns dar.async.promise)

(set! *warn-on-reflection* true)

(defprotocol IPromise
  (abort! [this])
  (deliver! [this val])
  (delivered? [this])
  (then [this cb])
  (value [this]))

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

(deftype Promise [state abort-cb]
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

  (then [this cb] (let [state* (swap! state #(if (:has-value? %) %
                                               (update-in % [:callbacks] conj cb)))]
                    (when (:has-value? state*)
                      (cb (:val state*)))))

  (value [this] (:val @state))

  (abort! [this] (when abort-cb
                   (abort-cb this))))

(defn new-promise
  ([] (new-promise nil))
  ([abort-cb] (Promise. (atom (->PromiseState nil false nil)) abort-cb)))
