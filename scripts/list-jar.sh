#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"
source "./config.sh"

cd "$ROOT"

./scripts/check-versions.sh

LEIN_VERSION=`cat "$PROJECT_FILE" | grep "defproject" | cut -d' ' -f3 | cut -d\" -f2`

JAR_FILE="target/devtools-$LEIN_VERSION.jar"

echo "listing content of $JAR_FILE"
echo ""

unzip -l "$JAR_FILE"

echo ""
echo "----------------------------"
echo ""
