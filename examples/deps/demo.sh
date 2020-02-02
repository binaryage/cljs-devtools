#!/usr/bin/env bash

set -x

clj --main cljs.main --compile-opts cljsc_opts.edn --compile app.core

python -m http.server 3300 --directory public
