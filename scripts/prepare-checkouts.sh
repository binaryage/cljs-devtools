#!/usr/bin/env bash

set -e

. "$(dirname "${BASH_SOURCE[0]}")/config.sh"

pushd "$PARENT_DIR"

if [ ! -d "cljs-devtools" ] ; then
  git clone "$CLJS_DEVTOOLS_REPO"
fi

if [ ! -d "lein-figwheel" ] ; then
  git clone "$LEIN_FIGWHEEL_REPO"
fi

pushd "cljs-devtools"
git checkout master
git pull
popd

pushd "lein-figwheel"
git checkout devtools
git pull
popd

pushd "$ROOT"

if [ ! -d "checkouts" ] ; then
  mkdir checkouts
fi

pushd "$ROOT/checkouts"

if [ ! -f "cljs-devtools" ] ; then
  ln -s ../../cljs-devtools cljs-devtools
fi

if [ ! -f "figwheel-sidecar" ] ; then
  ln -s ../../lein-figwheel/sidecar figwheel-sidecar
fi

if [ ! -f "figwheel-support" ] ; then
  ln -s ../../lein-figwheel/support figwheel-support
fi

popd

popd

popd