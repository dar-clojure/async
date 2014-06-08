#dar.async

##Motivation

[core.async](https://github.com/clojure/core.async) is heavily
focused on [CSP](http://en.wikipedia.org/wiki/Communicating_sequential_processes).
While CSP itself might be a good idea, it's certainly not the case that every async
problem is a CSP problem. For example, you often use `go` block just because
it allows to write non-blocking code in a regular linear fashion, not because it is
a funky way to get values from channels. For such case channels
are too heavy. When you have lots of async values coming from cache their
impact on performance might be unacceptable.

This library provides just a `go` block without unnecessary features.
We take it as-is from core.async, replace channels with lightweight promises
and remove all threadpool dispatch. Our `go` block runs synchronously (like a `do` block)
until it hits the first async point. Then if the result is immediately available
it just continues running in the current thread,
otherwise the execution will be resumed on a thread that delivered result.

##Examples

```clojure
(require '[dar.async :refer :all]
         '[dar.async.promise :refer :all])

;
; Promise
;

(let [p (new-promise)]
  (delivered? p) ; => false
  (deliver! p 1)
  (delivered? p) ; => true
  (value p) ; => 1
  (deliver! p 2)
  (value p) ; => 1 (ignors subsequent deliverings)
  (then p #(print %)) ; => "1" is printed immediately
  )

;
; Blocking wait
;

(do
  (<<! (doto (new-promise) (deliver! 1))) ; => 1
  (<<! (new-promise)) ; Deadlock here
  )

;
; Everything is a promise
;

(<<! 10) ; => 10

;
; Non-blocking wait (allowed only in a go block)
;

(let [p (new-promise)
      go-promise (go (+ 1 (<< p)))]
  (delivered? go-promise) ; => false
  (deliver! p 2)
  (value go-promise) ; => 3
  )

;
; Exceptions handling
;

(instance? Throwable
  (<<! (go
         (throw (Exception.))))) ; => true (go block catches all exceptions)

;
; "Trying waits"
;

; they are like << and <<!
(<?! (go (<? 1))) ; => 1
; except when the result is Throwable they rethrow it
(thrown? Exception (<?! (go
                          (<? (Exception.))))) ; => true

;
; aborts
;

(let [p (new-promise (fn on-abort [this]
                       (deliver! this (Exception. "Aborted!"))))
      result (go
               (<? p)
               true)]
  (abort! p)
  (instance? Exception
    (<<! result))) ; => true
```

##License

Copyright © 2014 Eldar Gabdullin

Distributed under the Eclipse Public License, the same as Clojure.
