# cljs-devtools [![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools) [![Build Status](https://travis-ci.org/binaryage/cljs-devtools.svg)](https://travis-ci.org/binaryage/cljs-devtools)

* Better presentation of ClojureScript values in Chrome DevTools (custom formatters)
* More informative exceptions (sanity hints)
* [Dirac DevTools](https://github.com/binaryage/dirac) support (dirac)

#### An example of formatting various ClojureScript data structures:

![](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sample-full.png)

#### Scope view / drawer Console (toggle with ESC)

![](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-scope.png)

#### An example of a sanity hint:

![An example of a sanity hint](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sanity-hint.png)

Read more in [v0.4.0 release notes](https://github.com/binaryage/cljs-devtools/releases/tag/v0.4.0).

## Integration in your own project

Add devtools dependency into your Leiningen's project.clj

[![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools)

To activate it. At some point you have to call `install!` from `devtools.core` namespace. Ideally run this at launch time of your app.

```clojure
(ns your-project.core
  (:require [devtools.core :as devtools]))

(devtools/enable-feature! :sanity-hints :dirac) ; enables additional features, :custom-formatters is enabled by default
(devtools/install!)

(.log js/console (range 200))
```

Check out the **[sample project](https://github.com/binaryage/cljs-devtools-sample)**.

## Enable Custom formatters in Chrome

**Available in [Chrome 47 and higher](http://googlechromereleases.blogspot.cz/2015/12/stable-channel-update.html)**

  * Open DevTools
  * Go to Settings ("three dots" icon in the upper right corner of DevTools > Menu > Settings `F1` > Console)
  * Check-in "Enable custom formatters"
  * Close DevTools
  * Open DevTools

## What next?

  * [Dirac DevTools](https://github.com/binaryage/dirac)

---

#### License [MIT](https://raw.githubusercontent.com/binaryage/cljs-devtools/master/LICENSE.txt)