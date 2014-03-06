(ns easy-app.async.promise
  (:import (java.lang Exception)))

(defprotocol IPromise
  (fulfil [this val])
  (fulfiled? [this])
  (then [this cb])
  (abort [this])
  (value [this]))

(extend-protocol IPromise
  nil
  (fulfiled? [_] true)
  (then [_ cb] (cb nil))
  (abort [_])
  (value [_] nil)

  java.lang.Object
  (fulfiled? [this] true)
  (then [this cb] (cb this))
  (abort [this])
  (value [this] this))

(defrecord State [val has-value? callbacks])

(deftype Promise [state]
  IPromise
  (fulfil [this val]
    (swap! state (fn [state]
                   (when (:has-value? state)
                     (throw (Exception. "Promise was already fulfiled.")))
                   (assoc state :val val :has-value? true)))
    (doseq [cb (:callbacks @state)]
      (cb val)) ;; TODO: think what to do with exceptions here
    (swap! state assoc :callbacks nil))

  (fulfiled? [this] (:has-value? @state))

  (then [this cb]
    (let [state* (swap! state #(if (:has-value? %)
                                 %
                                 (update-in % [:callbacks] conj cb)))]
      (when (:has-value? state*)
        (cb (:val state)))))

  (value [this] (:val @state)))

(defn make []
  (Promise. (atom (->State nil false []))))
