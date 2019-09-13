#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"
source "_config.sh"

PROFILES=${1:-"+testing"}

cd "$ROOT"

if [ -d "$DCE_CACHE_DIR" ] ; then
  rm -rf "$DCE_CACHE_DIR"
fi
mkdir -p "$DCE_CACHE_DIR"

# we have to aggressively clean all caches
# for some reason cljs compiler reuses compilation results between builds and that completely ruins our results

lein clean
lein with-profile "$PROFILES" cljsbuild once dce-no-debug
cp "$ROOT/test/resources/.compiled/dce-no-debug/build.js" "$DCE_CACHE_DIR/no-debug.js"

lein clean
lein with-profile "$PROFILES" cljsbuild once dce-with-debug
cp "$ROOT/test/resources/.compiled/dce-with-debug/build.js" "$DCE_CACHE_DIR/with-debug.js"

lein clean
lein with-profile "$PROFILES" cljsbuild once dce-no-mention
cp "$ROOT/test/resources/.compiled/dce-no-mention/build.js" "$DCE_CACHE_DIR/no-mention.js"

lein clean
lein with-profile "$PROFILES" cljsbuild once dce-no-require
cp "$ROOT/test/resources/.compiled/dce-no-require/build.js" "$DCE_CACHE_DIR/no-require.js"

lein clean
lein with-profile "$PROFILES" cljsbuild once dce-no-sources
cp "$ROOT/test/resources/.compiled/dce-no-sources/build.js" "$DCE_CACHE_DIR/no-sources.js"
