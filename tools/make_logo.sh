#!/bin/zsh
# Regenerates assets/logo.png (the README logo) from tools/make_logo.swift.
set -euo pipefail
cd "${0:A:h}/.."
TMP="$(mktemp -d)"
swiftc -O tools/make_logo.swift -o "$TMP/make_logo"
"$TMP/make_logo" assets/logo.png
rm -rf "$TMP"
