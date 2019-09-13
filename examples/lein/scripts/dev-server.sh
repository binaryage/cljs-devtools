#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
DEVSERVER_ROOT="$ROOT/resources/public"
DEVSERVER_PORT=7000

pushd "$DEVSERVER_ROOT"

python -m SimpleHTTPServer "$DEVSERVER_PORT"

popd