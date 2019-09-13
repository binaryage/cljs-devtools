#!/usr/bin/env bash

# updates all version strings

set -e -o pipefail

# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

cd "$ROOT"

VERSION=$1

if [ -z "$VERSION" ] ; then
  echo "please specify version as the first argument"
  exit 1
fi

sed -i "" -e "s/defproject binaryage\/devtools \".*\"/defproject binaryage\/devtools \"$VERSION\"/g" "$PROJECT_FILE"
sed -i "" -e "s/def current-version \".*\"/def current-version \"$VERSION\"/g" "$PROJECT_VERSION_FILE"

# this is just a sanity check
./scripts/check-versions.sh
