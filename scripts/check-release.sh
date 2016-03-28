#!/usr/bin/env bash

# checks if all version strings are consistent

set -e

. "$(dirname "${BASH_SOURCE[0]}")/config.sh"

pushd "$ROOT"

LEIN_VERSION=`cat "$PROJECT_FILE" | grep "defproject" | cut -d' ' -f3 | cut -d\" -f2`

JAR_FILE="target/devtools-$LEIN_VERSION.jar"

echo "listing content of $JAR_FILE"
unzip -l "$JAR_FILE"

echo "----------------------------"
echo ""

if [[ "$LEIN_VERSION" =~ "SNAPSHOT" ]]; then
  echo "Publishing SNAPSHOT versions is not allowed. Bump current version $LEIN_VERSION to a non-snapshot version."
  exit 2
fi

# http://stackoverflow.com/a/1885534/84283
echo "Are you sure to publish version ${LEIN_VERSION}? [Yy]"
read -n 1 -r
if [[ "$REPLY" =~ ^[Yy]$ ]]; then
  exit 0
else
  exit 1
fi

popd