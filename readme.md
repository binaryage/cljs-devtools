# cljs-devtools

**DANGER ZONE - WORK IN PROGRESS - EXPERIMENTAL APIs**

[![Build Status](https://travis-ci.org/binaryage/cljs-devtools.svg)](https://travis-ci.org/binaryage/cljs-devtools)

Better presentation of ClojureScript values in Chrome Devtools:

<img src="https://dl.dropboxusercontent.com/u/559047/cljs-formatter-prototype.png">

## Integration in your own project

Add devtools dependency into your Leiningen's project.clj:

[![Clojars Project](http://clojars.org/binaryage/devtools/latest-version.svg)](http://clojars.org/binaryage/devtools)

To activate it. At some point you have to call `install!` from `devtools.core` namespace. Ideally run this at launch time of your app.

    (ns your-project.core
      (:require [devtools.core :as devtools]))
    
    (devtools/install!)
    
    (.log js/console (range 200))

Note: version 0.2.2 is compatible with Chromium built against Blink [r194327 or higher](https://codereview.chromium.org/1096283003).

You can see Blink version which was compiled in your Chrome under **chrome://version** (url).

## See [sample project](https://github.com/binaryage/cljs-devtools-sample)

## Enable Custom formatters in your Chrome (Canary)

For now you must use Chrome Canary or bleeding edge build from [chromium-browser-snapshots](https://commondatastorage.googleapis.com/chromium-browser-snapshots/index.html).

##### First, enable Dev Tools Experiments:

  * Visit chrome://flags
  * Search the page for "devtools".
  * Click "Enable" under "Enable Developer Tools Experiments"
  * Click "Relaunch Now"

##### Then turn on custom formatters:

  * Open Dev Tools.
  * Click the gear in the upper right to go to Settings.
  * Click the "Experiments" tab.
  * Press the shift key six times to show the "hidden experiments".
  * Check "Custom Object Formatters".
  * Close Dev Tools.
  * Open Dev Tools.
  * Click the gear to open settings again.
  * Under "Console", check "Enable custom formatters".

#### License [MIT](https://raw.githubusercontent.com/binaryage/cljs-devtools/master/LICENSE.txt)