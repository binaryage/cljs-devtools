#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"
source "_config.sh"

cd "$ROOT"

./scripts/list-jar.sh

lein with-profile lib install
