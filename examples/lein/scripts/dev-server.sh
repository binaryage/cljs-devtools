#!/usr/bin/env bash

set -e -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

ROOT=$(pwd -P)
DEVSERVER_ROOT="$ROOT/resources/public"
DEVSERVER_PORT=7000

cd "$DEVSERVER_ROOT"

exec python -m http.server "$DEVSERVER_PORT"
