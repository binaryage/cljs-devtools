#!/usr/bin/env bash

set -e

COMPILED_PATH="resources/public/_compiled"
CONDITIONAL_INSTALL_OUTPUT="$COMPILED_PATH/advanced-conditional-install/devtools_sample.js"
UNCONDITIONAL_INSTALL_OUTPUT="$COMPILED_PATH/advanced-unconditional-install/devtools_sample.js"
NO_INSTALL_OUTPUT="$COMPILED_PATH/advanced-no-install/devtools_sample.js"

function keywords {
  echo "     file: $1"
  cat "$1" | perl -pe 's/\$\$/\$\$\n/g' | grep -o 'devtools\$.*' | sort | uniq -c
}

echo "no-install:"
keywords "$NO_INSTALL_OUTPUT"
echo ""
echo ""
echo "conditional-install:"
keywords "$CONDITIONAL_INSTALL_OUTPUT"
echo ""
echo ""
echo "unconditional-install:"
keywords "$UNCONDITIONAL_INSTALL_OUTPUT"