# cljs-devtools

**DANGER ZONE - WORK IN PROGRESS - EXPERIMENTAL APIs**

[![Build Status](https://travis-ci.org/binaryage/cljs-devtools.svg)](https://travis-ci.org/binaryage/cljs-devtools)

Better presentation of ClojureScript values in Chrome Devtools.

## Integration in your own project

Add devtools dependency into your Leiningen's project.clj:

[![Clojars Project](http://clojars.org/binaryage/devtools/latest-version.svg)](http://clojars.org/binaryage/devtools)

To activate it. At some point you have to call `install!` from `devtools.core` namespace. Ideally run this at launch time of your app.

    (ns your-project.core
      (:require [devtools.core :as devtools]))
    
    (devtools/install!)
    
    (.log js/console (range 200))
    
## See [sample project](https://github.com/binaryage/cljs-devtools-sample)

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-formatter-prototype.png">

## License

[MIT License](http://opensource.org/licenses/MIT)