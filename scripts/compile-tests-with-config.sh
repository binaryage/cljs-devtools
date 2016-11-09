#!/usr/bin/env bash

set -e

pushd `dirname "${BASH_SOURCE[0]}"` > /dev/null
source "./config.sh"

pushd "$ROOT"

env CLJS_DEVTOOLS/FN_SYMBOL=X \
    CLJS_DEVTOOLS/SOME_UNUSED_CONFIG_TWEAK=value \
    CLJS_DEVTOOLS/SOME_UNUSED_CONFIG_TWEAK2=true \
    lein with-profile +testing cljsbuild once tests-with-config

popd

popd
