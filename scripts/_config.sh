#!/usr/bin/env bash

set -e -o pipefail

pushd() {
  command pushd "$@" >/dev/null
}

popd() {
  command popd >/dev/null
}

filesize() {
  wc -c <"$1" | xargs
}

read_lein_version() {
  < "$1" grep "defproject" | cut -d' ' -f3 | cut -d\" -f2
}

read_project_version() {
  < "$1" grep "(def current-version" | cut -d" " -f3 | cut -d\" -f2
}

pushd .

cd "$(dirname "${BASH_SOURCE[0]}")/.."

ROOT=$(pwd)
SCRIPTS="$ROOT/scripts"
PROJECT_VERSION_FILE="src/lib/devtools/version.clj"
PROJECT_FILE="project.clj"
CACHE_DIR="$ROOT/.cache"
DCE_CACHE_DIR="$CACHE_DIR/dce"
DCE_COMPARE_DIR="$CACHE_DIR/dce-compare"
SKIP_DCE_COMPILATION=${SKIP_DCE_COMPILATION}
EXAMPLE_LEIN_PROJECT_FILE="examples/lein/project.clj"

popd
