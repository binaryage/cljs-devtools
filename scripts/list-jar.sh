#!/usr/bin/env bash

set -e

pushd `dirname "${BASH_SOURCE[0]}"` > /dev/null
source "./config.sh"

pushd "$ROOT"

./scripts/check-versions.sh

LEIN_VERSION=`cat "$PROJECT_FILE" | grep "defproject" | cut -d' ' -f3 | cut -d\" -f2`

JAR_FILE="target/devtools-$LEIN_VERSION.jar"

echo "listing content of $JAR_FILE"
echo ""

unzip -l "$JAR_FILE"

echo ""
echo "----------------------------"
echo ""

popd

popd
