# CLJS DevTools Installation

## Enable Custom formatters in the browser

### In Chrome and Edge

Available in [**Chrome 47 and higher**](https://googlechromereleases.blogspot.cz/2015/12/stable-channel-update.html) and **Edge 79 and higher**.

~~**WARNING**: Custom formatters will be probably removed from Chrome 86 (or later).~~
~~Please read recent news in [issue 55](https://github.com/binaryage/cljs-devtools/issues/55).~~
~~Newly since Chrome 84 you have to re-enable custom formatters every time you open devtools. The setting is not sticky anymore.~~ 

UPDATE:

Chrome developers [reconsidered removing the custom formatters feature](https://github.com/binaryage/cljs-devtools/issues/55#issuecomment-632384007).  

All should work the same as before for now. If your setting is not sticky, please make sure you are using the latest Chrome (Canary) update.

  * Open DevTools
  * Go to Settings ("three dots" icon in the upper right corner of `DevTools > Menu > Settings F1 > Preferences > Console`)
  * Check "Enable custom formatters"
  * Close DevTools
  * Open DevTools

Note: You might need to refresh the page first time you open Console panel with existing logs - custom formatters are applied
only to newly printed console messages.

### In Firefox

Available in [**Firefox 111 and higher**](https://www.mozilla.org/en-US/firefox/111.0/releasenotes/).

  * Open DevTools
  * Go to Settings ("three dots" icon in the upper right corner of `DevTools > Menu > Settings F1 > Advanced settings`)
  * Check "Enable custom formatters"
  * Close DevTools
  * Open DevTools

Note: You might need to refresh the page first time you open Console panel with existing logs - custom formatters are applied
only to newly printed console messages.

## Integrate with your project

Add devtools dependency into your Leiningen's `project.clj` or boot file.

[![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools)

### Development builds

CLJS devtools is meant to be used only with development builds.

In general you have two options how to integrate CLJS DevTools with your project:

#### Install it via `:preloads`

ClojureScript [supports](http://dev.clojure.org/jira/browse/CLJS-1688) `:preloads` [compiler option](https://github.com/clojure/clojurescript/wiki/Compiler-Options#preloads)
which allows you to require namespaces prior your `:main` namespace. This means that you can use this feature to add cljs-devtools support
to your project without modification of your code. You simply add `devtools.preload` into the `:preloads` list.

#### Install it manually

You call `install!` it from `devtools.core` namespace.
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

### Advanced builds

Because `:formatters` feature of CLJS devtools does not work under `:compiler {:optimizations :advanced}` you will
 probably want to completely exclude the library from your production builds.

In general you have three options:

  - Use `:preloads`. Preloads are not included in advanced builds.
  - Use different `:source-paths` for development and production builds. Production build should have no reference to devtools namespaces.
  - Do not call `(devtools.core/install!)`, make it obvious to closure optimizer somehow and rely on advanced optimizations dead code elimination
  to remove whole library as a dead code. You can simply use some macro which won't emit `(devtools.core/install!)` call under advanced builds.
  Or more elegantly you can use `:closure-defines` to define a conditional which will be understood by closure optimizer. This technique was
  [discussed here](https://github.com/binaryage/cljs-devtools/releases/tag/v0.5.3). You can also look into [project.clj](../project.clj)
  and check out the `:dead-code-elimination` build. RECENT DISCOVERY: This method does not work reliably as we have discovered in [issue 37](https://github.com/binaryage/cljs-devtools/issues/37).

### Configuration

You can enable/disable desired features and configure other aspects of CLJS DevTools. Please refer to a separate documentation
on [configuring the library](https://github.com/binaryage/cljs-devtools/blob/master/docs/configuration.md).

### Additional notes

* For inspiration, you might want to check an **[example Leiningen project](https://github.com/binaryage/cljs-devtools/tree/master/examples/lein)** out.
* Boot users might want consider using [boot-cljs-devtools](https://github.com/boot-clj/boot-cljs-devtools)).
