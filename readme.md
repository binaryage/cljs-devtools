# cljs-devtools 

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](license.txt)
[![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools) 
[![Example Projects](https://img.shields.io/badge/project-examples-ff69b4.svg)](https://github.com/binaryage/cljs-devtools/tree/master/examples)

CLJS DevTools adds enhancements into Chrome, Edge and Firefox DevTools for ClojureScript developers:

* Better presentation of ClojureScript values in DevTools (see the [:formatters][1] feature)
* More informative exceptions (see the [:hints][2] feature)
* Long stack traces for chains of async calls (see the [:async][3] feature)

### Documentation

* [**FAQ**](docs/faq.md)
* [**Installation**](docs/installation.md)
* [**Configuration**](docs/configuration.md)
* [**Examples**](examples)

#### An example of formatting ClojureScript values:

![Custom formatters in action](https://box.binaryage.com/cljs-devtools-sample-full.png)

#### An example of improved exceptions:

![An example of hints](https://box.binaryage.com/cljs-devtools-sanity-hint.png)

[1]: https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#what-is-the-formatters-feature
[2]: https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#what-is-the-hints-feature
[3]: https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#what-is-the-async-feature
