#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"
source "./config.sh"

cd "$ROOT"

env CLJS_DEVTOOLS/FN_SYMBOL=X \
    CLJS_DEVTOOLS/SOME_UNUSED_CONFIG_TWEAK=value \
    CLJS_DEVTOOLS/SOME_UNUSED_CONFIG_TWEAK2=true \
    lein with-profile +testing cljsbuild once tests-with-config