#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"
source "_config.sh"

cd "$ROOT"

if [[ -z "${SKIP_DCE_COMPILATION}" ]]; then
  ./scripts/compile-dead-code.sh "$@"
fi

cd "$DCE_CACHE_DIR"

BUILD_WITH_DEBUG="$DCE_CACHE_DIR/with-debug.js"
BUILD_NO_DEBUG="$DCE_CACHE_DIR/no-debug.js"
BUILD_NO_MENTION="$DCE_CACHE_DIR/no-mention.js"
BUILD_NO_REQUIRE="$DCE_CACHE_DIR/no-require.js"
BUILD_NO_SOURCES="$DCE_CACHE_DIR/no-sources.js"

WITH_DEBUG_SIZE=$(filesize "$BUILD_WITH_DEBUG")
NO_DEBUG_SIZE=$(filesize "$BUILD_NO_DEBUG")
NO_MENTION_SIZE=$(filesize "$BUILD_NO_MENTION")
NO_REQUIRE_SIZE=$(filesize "$BUILD_NO_REQUIRE")
NO_SOURCES_SIZE=$(filesize "$BUILD_NO_SOURCES")

echo
echo "stats:"
echo "WITH_DEBUG: $WITH_DEBUG_SIZE bytes"
echo "NO_DEBUG:   $NO_DEBUG_SIZE bytes"
echo "NO_MENTION: $NO_MENTION_SIZE bytes"
echo "NO_REQUIRE: $NO_REQUIRE_SIZE bytes"
echo "NO_SOURCES: $NO_SOURCES_SIZE bytes"
echo

if [ -d "$DCE_COMPARE_DIR" ] ; then
  rm -rf "$DCE_COMPARE_DIR"
fi

mkdir -p "$DCE_COMPARE_DIR"

cd "$DCE_COMPARE_DIR"

js-beautify -f "$BUILD_WITH_DEBUG" -o "with-debug.js"
js-beautify -f "$BUILD_NO_DEBUG" -o "no-debug.js"
js-beautify -f "$BUILD_NO_MENTION" -o "no-mention.js"
js-beautify -f "$BUILD_NO_REQUIRE" -o "no-require.js"
js-beautify -f "$BUILD_NO_SOURCES" -o "no-sources.js"

echo
echo "beautified sources in $DCE_COMPARE_DIR"
echo

echo "see https://github.com/binaryage/cljs-devtools/issues/37"
