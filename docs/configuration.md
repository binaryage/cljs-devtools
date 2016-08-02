# CLJS DevTools Configuration

You can enable/disable desired features and configure other aspects of CLJS DevTools.

CLJS DevTools has sane defaults. But if you need to, you can specify a config map overriding default configuration keys.

Here is the [list of default configuration keys](https://github.com/binaryage/cljs-devtools/blob/master/src/lib/devtools/defaults.cljs).

### Configuration via `:preloads`

When [installed via preloads](https://github.com/binaryage/cljs-devtools/blob/master/docs/installation.md#install-it-via-preloads), 
you can specify a config map in your `project.clj` or `build.boot` under cljs compiler options. 

For example:

```clojure
...
:compiler {
  :output-to       "..."
  :output-dir      "..."
  :main            ...
  :preloads        [devtools.preload ...]
  :external-config {
    :devtools/config {
      :features-to-install    [:formatters :hints]
      :fn-symbol              "F"
      :print-config-overrides true}}
  ...}
```

This overrides default `:features-to-install`, sets custom `:fn-symbol` and 
instructs cljs-devtools to print overridden config values during installation.

### Manual installation

When [installed manually](https://github.com/binaryage/cljs-devtools/blob/master/docs/installation.md#install-it-manually), 
you can pass a list of desired features to enable into `devtools.core.install!` call 
or use `devtools.core.set-pref!` to override individual default config keys prior calling `install!` from `devtools.core` namespace.
 
This explicit init code would be equivalent to the config map above.

```clojure
(ns your-project.devtools
  (:require [devtools.core :as devtools]))

(devtools/set-pref! :fn-symbol "F")
(devtools/set-pref! :print-config-overrides true)
(devtools/install! [:formatters :hints])
```

Here is the [list of default configuration keys](https://github.com/binaryage/cljs-devtools/blob/master/src/lib/devtools/defaults.cljs).
