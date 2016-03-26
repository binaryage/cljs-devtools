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

CLJS devtools is meant to be used only under development builds. You call `install!` it from `devtools.core` namespace.
A good technique is to use an independent namespace and require it before your core namespace (but after goog/base.js):

```clojure
(ns your-project.devtools
  (:require [devtools.core :as devtools]))

(devtools/install!)

(.log js/console (range 200))
```

Your dev index.html could look like this:

```html
<!doctype html>
<html>
<head>
    <script src="compiled/goog/base.js" type="text/javascript"></script>
    <script src="compiled/your_project.js" type="text/javascript"></script>
</head>
<body>
<script>goog.require('your_project.devtools')</script>
<script>goog.require('your_project.core')</script>
...
</body>
</html>
```

This will ensure that `devtools/install!` is called before your normal code gets any chance to run. It does not rely on
namespace dependencies where you cannot force exact ordering and it will work even if you happen to run side-effecting code
during requiring your code or libraries (for example you are logging something to javascript console during namespace require).

By default only `:custom-formatters` feature is installed. You can call `install!` with explicit list of features to enable.

```clojure
(devtools/install! [:custom-formatters :sanity-hints]) ; to enable all features
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