# CLJS DevTools Configuration

You can enable/disable desired features and configure other aspects of cljs-devtools.

Configuration can be done on multiple levels (later overrides former levels):

1. defaults
2. compiler options
3. environmental variables
4. programmatically

### Configuration via defaults

Here is the [list of default configuration keys](https://github.com/binaryage/cljs-devtools/blob/master/src/lib/devtools/defaults.cljs).

### Configuration via compiler options

You can specify a config map in your `project.clj` or `build.boot` under ClojureScript compiler options. 

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

This overrides default `:features-to-install`, sets custom `:fn-symbol` and instructs cljs-devtools to print overridden config 
values during installation.

### Configuration via environmental variables

We use [binaryage/env-config](https://github.com/binaryage/env-config) library to allow configuration overrides via 
environmental variables. The prefix for our configuration variables is `CLJS_DEVTOOLS`.

For example, in Bash shell you can do something like to achieve configuration matching the config map above:

    lein clean
    env CLJS_DEVTOOLS/FN_SYMBOL=F \
      CLJS_DEVTOOLS/PRINT_CONFIG_OVERRIDES=true \
      CLJS_DEVTOOLS/FEATURES_TO_INSTALL="~[:formatters :hints]" \
      lein cljsbuild once
      
Please note that environmental variables are retrieved during compilation in macro expansion. So they must be present
during ClojureScript compilation and you have to reset compilation caches after changing the environment.

### Programmatic configuration

When [installed manually](https://github.com/binaryage/cljs-devtools/blob/master/docs/installation.md#install-it-manually), 
you can pass a list of desired features to enable into `devtools.core/install!` call 
or use `devtools.core/set-pref!` to override individual default config keys prior calling `install!` from `devtools.core` 
namespace.
 
This explicit init code would be equivalent to the config map above.

```clojure
(ns your-project.devtools
  (:require [devtools.core :as devtools]))

(devtools/set-pref! :fn-symbol "F")
(devtools/set-pref! :print-config-overrides true)
(devtools/install! [:formatters :hints])
```

Please note that most configuration keys can be changed dynamically even after cljs-devtools installation. 
For example styling options can be redefined temporarily for desired styling tweaks isolated for a single `console.log` call.

Here is the [list of default configuration keys](https://github.com/binaryage/cljs-devtools/blob/master/src/lib/devtools/defaults.cljs).
