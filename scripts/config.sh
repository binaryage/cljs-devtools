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
PROJECT_VERSION_FILE="src/devtools/version.clj"
PROJECT_FILE="project.clj"

popd