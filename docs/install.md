# CLJS DevTools Installation

## Enable Custom formatters in Chrome

Available in [**Chrome 47 and higher**](http://googlechromereleases.blogspot.cz/2015/12/stable-channel-update.html).

  * Open DevTools
  * Go to Settings ("three dots" icon in the upper right corner of `DevTools > Menu > Settings F1 > General > Console`)
  * Check-in "Enable custom formatters"
  * Close DevTools
  * Open DevTools

Note: You might need to refresh the page first time you open Console panel with existing logs - custom formatters are applied
only to newly printed console messages.

## Integrate with your project

Add devtools dependency into your Leiningen's `project.clj`

[![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools)

### Dev builds

CLJS devtools is meant to be used only with development builds. You call `install!` it from `devtools.core` namespace.
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

The [list of all `known-features` is here](https://github.com/binaryage/cljs-devtools/blob/master/src/devtools/core.cljs#L9).

Check out the **[sample project](https://github.com/binaryage/cljs-devtools-sample)**.

### Advanced builds

Because `:custom-formatters` feature of CLJS devtools does not work under `:compiler {:optimizations :advanced}` you will
 probably want to completely exclude the library from your production builds.

In general you have two options:

  - Use different `:source-paths` for development and production builds. Production build should have no reference to devtools namespaces.
  - Do not call `(devtools.core/install!)`, make it obvious to closure optimizer somehow and rely on advanced optimizations dead code elimination
  to remove whole library as a dead code. You can simply use some macro which won't emit `(devtools.core/install!)` call under advanced builds.
  Or more elegantly you can use `:closure-defines` to define a conditional which will be understood by closure optimizer. This technique was
  [discussed here](https://github.com/binaryage/cljs-devtools/releases/tag/v0.5.3). You can also look into [project.clj](../project.clj)
  and check out the `:dead-code-elimination` build.