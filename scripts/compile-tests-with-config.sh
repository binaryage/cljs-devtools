#!/usr/bin/env bash

set -e -o pipefail

# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

cd "$ROOT"

env CLJS_DEVTOOLS/FN_SYMBOL=X \
    CLJS_DEVTOOLS/SOME_UNUSED_CONFIG_TWEAK=value \
    CLJS_DEVTOOLS/SOME_UNUSED_CONFIG_TWEAK2=true \
    lein with-profile +testing cljsbuild once tests-with-config