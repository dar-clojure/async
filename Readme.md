#dar.async [![Clojars Project](http://clojars.org/dar/async/latest-version.svg)](http://clojars.org/dar/async)

We take a `go` block from [core.async](https://github.com/clojure/core.async)
as is, remove all threadpool dispatch and replace channels with lightweight
[promises](https://github.com/dar-clojure/async.promise).
Our `go` block runs synchronously (like a `do` block) until it hits the first async point.
Then, if the result is immediately available,
it just continues running on the same thread,
otherwise execution will be resumed on a thread that delivered result.
Basically, this is how C# works.

The main reason is a performance. Sometimes you want
to write non-blocking code not because it is really asynchronous,
but because it might be asynchronous, or may be you have a lot's
of values coming from cache. For such cases [core.async](https://github.com/clojure/core.async)
is unacceptably slow. Abortable computations is another case
not covered quite by core.async.

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

##Installation

Available via [Clojars](https://clojars.org/dar/async)

##License

Copyright © 2014 Eldar Gabdullin, Rich Hickey & contributors

Distributed under the Eclipse Public License, the same as Clojure.
