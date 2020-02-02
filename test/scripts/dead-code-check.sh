#!/usr/bin/env bash

# ensure that advanced build has no traces of devtools namespace references

set -e

COMPILED_PATH="test/resources/.compiled/dead-code/build.js"

STATS=$(perl -pe 's/(\\$|\\d+)\\$/\\1\\$\\n/g' <"$COMPILED_PATH" | grep -o 'devtools\\$.*' | sort | uniq -c)

if [[ -z "$STATS" ]]; then
  echo "Compiled file '$COMPILED_PATH' contains no traces of devtools (as expected)."
  exit 0
else
  echo "Compiled file '$COMPILED_PATH' contains symbols which were expected to be removed as a dead code during :advanced build"
  echo "$STATS"
  exit 1
fi
