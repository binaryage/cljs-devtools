# cljs-devtools [![GitHub license](https://img.shields.io/github/license/binaryage/cljs-devtools.svg)](license.txt) [![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools) [![Travis](https://img.shields.io/travis/binaryage/cljs-devtools.svg)](https://travis-ci.org/binaryage/cljs-devtools) [![Sample Project](https://img.shields.io/badge/project-example-ff69b4.svg)](https://github.com/binaryage/cljs-devtools-sample)

* Better presentation of ClojureScript values in Chrome DevTools (`:custom-formatters` feature)
* More informative exceptions (`:sanity-hints` feature)

#### An example of formatting various ClojureScript data structures:

![](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sample-full.png)

#### Scope view / drawer Console (toggle with ESC)

![](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-scope.png)

#### An example of a sanity hint:

![An example of a sanity hint](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sanity-hint.png)

Read more in [v0.4.0 release notes](https://github.com/binaryage/cljs-devtools/releases/tag/v0.4.0).

## Integration in your own project

Add devtools dependency into your Leiningen's `project.clj`

[![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools)

To install it. You have to call `install!` from `devtools.core` namespace. Ideally run this as early as possible during
launch time of your app.

```clojure
(ns your-project.core
  (:require [devtools.core :as devtools]))

; this enables additional features, :custom-formatters is enabled by default
(devtools/enable-feature! :sanity-hints)
(devtools/install!)

(.log js/console (range 200))
```

Check out the **[sample project](https://github.com/binaryage/cljs-devtools-sample)**.

## Enable Custom formatters in Chrome

Available in [**Chrome 47 and higher**](http://googlechromereleases.blogspot.cz/2015/12/stable-channel-update.html).

  * Open DevTools
  * Go to Settings ("three dots" icon in the upper right corner of `DevTools > Menu > Settings F1 > General > Console`)
  * Check-in "Enable custom formatters"
  * Close DevTools
  * Open DevTools

Note: You might need to refresh the page first time you open Console panel with existing logs - custom formatters are applied
only to newly printed console messages.

## What next?

  * [Dirac DevTools](https://github.com/binaryage/dirac)