#!/usr/bin/env bash

pushd () {
    command pushd "$@" > /dev/null
}

popd () {
    command popd "$@" > /dev/null
}

pushd .

cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
PARENT_DIR="$ROOT/.."

CLJS_DEVTOOLS_REPO="https://github.com/binaryage/cljs-devtools.git"
LEIN_FIGWHEEL_REPO="https://github.com/darwin/lein-figwheel.git"

popd .