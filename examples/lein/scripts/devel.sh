#!/usr/bin/env bash

set -e -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

set -x

export DEVTOOLS_DEBUG=1

lein with-profile +demo,+checkouts,+devel,+figwheel figwheel
