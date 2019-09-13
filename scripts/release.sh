#!/usr/bin/env bash

set -e -o pipefail

# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

cd "$ROOT"

lein clean

cd "$SCRIPTS"

./check-versions.sh
./prepare-jar.sh
./check-release.sh
./deploy-clojars.sh
