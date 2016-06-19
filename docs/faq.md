# CLJS DevTools FAQ

### What is the `:custom-formatters` feature?

You can log specific javascript object types with your own printing routine.
Basically you can register a javascript handler which will be called by
DevTools javascript console to present object in the console. This handler
can output rich formatting / expandable-structure (JsonML).

Read more in [this Google Doc](https://docs.google.com/document/d/1FTascZXT9cxfetuPRT2eXPQKXui4nWFivUnS_335T3U).

### Why some custom formatters were not rendered?

First, custom formatters must be enabled in DevTools Settings:

`> DevTools menu > Settings (F1) > Console > Enable custom formatters`

The feature is disabled by default and it is easy to forget to enable it after starting with a new Chrome profile.

Second, please note that custom formatters is a feature of DevTools UI, but console logging is a general feature of Javascript runtime.
When logging happens while DevTools is not attached a different logging system is used for recoding console logs in background.
When DevTools later attaches, existing recorded logs are replayed in the DevTools Console UI. Unfortunately during this replay process
no custom formatters are applied, so you can see only raw logs. There are probably some technical reasons for this behaviour.

To see properly formatted logs you have to refresh your page while DevTools are attached. Any newly printed logs should have
custom formatters applied assuming you have enabled the feature in the DevTools Settings.

This behaviour caused some confusion among users so I implemented a detection and since v0.7 we print a warning
when custom formatters seem not to get rendered.

You can disable this warning by setting this pref prior installation:

```clojure
(devtools.core/set-pref! :dont-detect-custom-formatters true)
```

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

You can enable the feature when calling `install!`:

```clojure
(devtools.core/install! [:custom-formatters :sanity-hints])
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
