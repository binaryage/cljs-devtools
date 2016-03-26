# CLJS DevTools FAQ

### What is the `:custom-formatters` feature?

You can log specific javascript object types with your own printing routine.
Basically you can register a javascript handler which will be called by
DevTools javascript console to present object in the console. This handler
can output rich formatting / expandable-structure (JsonML).

Read more in [this Google Doc](https://docs.google.com/document/d/1FTascZXT9cxfetuPRT2eXPQKXui4nWFivUnS_335T3U).

### What is the `:sanity-hints` feature?

Sometimes your DevTools displays cryptic errors like `Cannot read property 'call' of null`. The problem is in the way how ClojureScript compiler emits function calls.

Sanity hints is an attempt to augment uncaught exceptions and error object to include a bit of additional knowledge related to such errors.
It tries to fetch the original source file, extract relevant part to show you more context and mark javascript error there.
This is expected to work only with `:optimizations none` compiler mode and it is disabled by default because it relies on monkey patching.
But it is worth it:

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sanity-hint.png">

Note `<<< ☢ RETURNED NULL ☢ <<< ` part which points to error location. The uncaught error was raised by calling `sanity-test-handler` in the following code:

```clojure
(defn fn-returns-nil [])

(defn sanity-test-handler []
  ((fn-returns-nil) "param"))
```

You can enable the feature by setting this pref prior calling `install!`:

```clojure
(devtools/install! [:custom-formatters :sanity-hints])
```

Technical details are described in [the source file](https://github.com/binaryage/cljs-devtools/blob/master/src/devtools/sanity_hints.cljs).

### Why custom formatters do not work for advanced builds?

There is a technical glitch which currently prevents CLSJ devtools to work under
:advanced optimizations. Some [ClojureScript type hints are not preserved in advanced builds](http://dev.clojure.org/jira/browse/CLJS-1249)
and that is why CLJS DevTools cannot recognize objects belonging to CLJS land.

Philosophically you should not include debug/diagnostics code in your production builds anyways.

### How do I elide the library in my :advanced builds?

Please read [install.md#advanced-builds](install.md#advanced-builds).

### Does this work in other browsers than Chrome?

No, AFAIK.

### What else can I do to improve my ClojureScript development experience?

Check out [Dirac DevTools](https://github.com/binaryage/dirac) which is
a custom fork of Chrome DevTools which goes one or two steps further.
