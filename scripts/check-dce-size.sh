#!/usr/bin/env bash

set -e

pushd `dirname "${BASH_SOURCE[0]}"` > /dev/null
source "./config.sh"

cd "$ROOT"

if [[ -z "${SKIP_DCE_COMPILATION}" ]]; then
  ./scripts/compile-dead-code.sh "$@"
fi

cd "$DCE_CACHE_DIR"

NO_DEBUG_MAX_SIZE=6000
NO_REQUIRE_MAX_SIZE=6000

BUILD_WITH_DEBUG="$DCE_CACHE_DIR/with-debug.js"
BUILD_NO_DEBUG="$DCE_CACHE_DIR/no-debug.js"
BUILD_NO_MENTION="$DCE_CACHE_DIR/no-mention.js"
BUILD_NO_REQUIRE="$DCE_CACHE_DIR/no-require.js"
BUILD_NO_SOURCES="$DCE_CACHE_DIR/no-sources.js"

WITH_DEBUG_SIZE=$(stat -f %z "$BUILD_WITH_DEBUG")
NO_DEBUG_SIZE=$(stat -f %z "$BUILD_NO_DEBUG")
NO_MENTION_SIZE=$(stat -f %z "$BUILD_NO_MENTION")
NO_REQUIRE_SIZE=$(stat -f %z "$BUILD_NO_REQUIRE")
NO_SOURCES_SIZE=$(stat -f %z "$BUILD_NO_SOURCES")

echo
echo "stats:"
echo "WITH_DEBUG: $WITH_DEBUG_SIZE bytes"
echo "NO_DEBUG:   $NO_DEBUG_SIZE bytes"
echo "NO_MENTION: $NO_MENTION_SIZE bytes"
echo "NO_REQUIRE: $NO_REQUIRE_SIZE bytes"
echo "NO_SOURCES: $NO_SOURCES_SIZE bytes"
echo

if [[ "$NO_DEBUG_SIZE" != "$NO_MENTION_SIZE" ]]; then
  echo "failure: NO_DEBUG and NO_MENTION expected to have the same size."
  exit 1
fi

if [[ "$NO_REQUIRE_SIZE" != "$NO_SOURCES_SIZE" ]]; then
  echo "failure: NO_REQUIRE and NO_SOURCES expected to have the same size."
  exit 2
fi

if [[ "$NO_DEBUG_SIZE" -gt "$NO_DEBUG_MAX_SIZE" ]]; then
  echo "failure: NO_DEBUG expected to have size smaller than $NO_DEBUG_MAX_SIZE bytes."
  exit 3
fi

if [[ "$NO_REQUIRE_SIZE" -gt "$NO_REQUIRE_MAX_SIZE" ]]; then
  echo "failure: NO_REQUIRE_SIZE expected to have size smaller than $NO_REQUIRE_MAX_SIZE bytes."
  exit 4
fi

popd
