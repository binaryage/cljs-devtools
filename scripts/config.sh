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
PROJECT_VERSION_FILE="src/lib/devtools/version.clj"
PROJECT_FILE="project.clj"
DCE_CACHE_DIR="$ROOT/.cache/dce"
DCE_COMPARE_DIR="$ROOT/test/resources/.compiled/dead-code-compare"
SKIP_DCE_COMPILATION=

popd
