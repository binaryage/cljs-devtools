# cljs-devtools-sample [![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](license.txt)

This project is an example of integration of [**cljs-devtools**](https://github.com/binaryage/cljs-devtools) into a
Leiningen-based ClojureScript project.

![](https://box.binaryage.com/cljs-devtools-sample-full.png)

## Local setup

    git clone https://github.com/binaryage/cljs-devtools.git
    cd cljs-devtools/examples/lein

Build the project and start a local demo server:

    lein demo

Wait for compilation and when figwheel fully starts:

  * A demo page should be available at [http://localhost:7000](http://localhost:7000).
  * Please visit it with Google Chrome, Microsoft Edge or Mozilla Firefox browser with [enabled custom formatters](https://github.com/binaryage/cljs-devtools).
  * Open the web development console under devtools and you should see something similar to the screenshot above.

Note: you might need to refresh the page again to force re-rendering of custom formatters after opening the console.
