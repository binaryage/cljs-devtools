#!/usr/bin/env bash

set -e -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

set -x

mkdir checkouts
cd checkouts

ln -s ../../.. cljs-devtools
