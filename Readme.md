#dar.async

##Motivation

[core.async](https://github.com/clojure/core.async) provides a nice
`go` block which allows you to write asynchronous non-blocking code
in a regular blocking manner. However, it's "future value" abstraction
is quite complex and heavy. In this library we replace [core.async](https://github.com/clojure/core.async)
channels with lightweight promises. Thread dispatch semantics is also changed.
When "async" value is ready, then execution is just continues on the same thread.
Otherwise, the go block will be suspended and resumed on a thread that delivered async value in question.
In other words, the difference between core.async and dar.async could be summarized as:
core.async is similar to [Go](http://golang.org/), dar.async is similar to
[C#](http://msdn.microsoft.com/en-us/library/vstudio/hh191443.aspx).
