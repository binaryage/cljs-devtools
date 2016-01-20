# cljs-devtools-sample [![MIT License](https://img.shields.io/badge/license-MIT-007EC7.svg?style=flat-square)](/license.txt)

This project is an example of integration of [**cljs-devtools**](https://github.com/binaryage/cljs-devtools) into a
Leiningen-based ClojureScript project.

![](https://dl.dropboxusercontent.com/u/559047/cljs-devtools-sample-full.png)

## Local setup

    git clone https://github.com/binaryage/cljs-devtools-sample.git
    cd cljs-devtools-sample

Build the project and start local demo server:

    lein demo

  * A demo web site should be available at [http://localhost:7000](http://localhost:7000).
  * Please go to your Chrome with [enabled custom formatters](https://github.com/binaryage/cljs-devtools).
  * Open the web development console under devtools and you should see something similar to the screenshot at the top.
    Note: you might have to refresh the page again to see custom formatters after opening the console.