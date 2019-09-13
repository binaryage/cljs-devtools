#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"
source "./config.sh"

cd "$ROOT"

lein with-profile lib jar
