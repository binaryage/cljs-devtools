# cljs-devtools 

[![GitHub license](https://img.shields.io/github/license/binaryage/cljs-devtools.svg)](license.txt) 
[![Clojars Project](https://img.shields.io/clojars/v/binaryage/devtools.svg)](https://clojars.org/binaryage/devtools) 
[![Travis](https://img.shields.io/travis/binaryage/cljs-devtools.svg)](https://travis-ci.org/binaryage/cljs-devtools) 
[![Sample Project](https://img.shields.io/badge/project-example-ff69b4.svg)](https://github.com/binaryage/cljs-devtools-sample)

CLJS DevTools is a collection of Chrome DevTools enhancements for ClojureScript developers:

  * Better presentation of ClojureScript values in Chrome DevTools (see [:formatters][1] feature)
  * More informative exceptions (see [:hints][2] feature)
  * Long stack traces for chains of async calls (see [:async][3] feature)

### Documentation

* [**FAQ**](docs/faq.md)
* [**Installation**](docs/installation.md)
* [**Configuration**](docs/configuration.md)

#### An example of formatting ClojureScript values with `:formatters` feature:

![Custom formatters in action](https://box.binaryage.com/cljs-devtools-sample-full.png)

#### An example of improved exceptions with `:hints` feature:

![An example of hints](https://box.binaryage.com/cljs-devtools-sanity-hint.png)

---

## What next?

  * [Dirac DevTools](https://github.com/binaryage/dirac)

[1]: https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#what-is-the-formatters-feature
[2]: https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#what-is-the-hints-feature
[3]: https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#what-is-the-async-feature
