# cljs-devtools

[![Build Status](https://travis-ci.org/binaryage/cljs-devtools.svg)](https://travis-ci.org/binaryage/cljs-devtools)

* Better presentation of ClojureScript values in Chrome DevTools
* [optional] More informative exceptions (sanity hints)

#### An example of formatting various ClojureScript data structures:

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-formatter-prototype.png">

#### Scope view / drawer Console (toggle with ESC)

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-devtools-scope.png">

#### An example of a sanity hint:

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sanity-hint.png">

## Integration in your own project

Add devtools dependency into your Leiningen's project.clj:

[![Clojars Project](http://clojars.org/binaryage/devtools/latest-version.svg)](http://clojars.org/binaryage/devtools)

To activate it. At some point you have to call `install!` from `devtools.core` namespace. Ideally run this at launch time of your app.

```clojure
(ns your-project.core
  (:require [devtools.core :as devtools]))

(devtools/set-pref! :install-sanity-hints true) ; this is optional
(devtools/install!)

(.log js/console (range 200))
```

See [the sample project](https://github.com/binaryage/cljs-devtools-sample).

## Enable Custom formatters in your Chrome (Canary)

For now you must use Chrome Canary (worked in 48.0.2537.0 canary (64-bit) under Mac).

##### Turn on custom formatters:

  * Open DevTools.
  * Go to Settings: (Click the "three dots" icon in the upper right corner of DevTools > Menu > Settings [F1])
  * Check "Enable custom formatters".
  * Close DevTools.
  * Open DevTools.

#### License [MIT](https://raw.githubusercontent.com/binaryage/cljs-devtools/master/LICENSE.txt)