#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 TrendVidia, LLC.
#
# Holds src/test/resources/text/ to the reference. GoConformanceTest compares
# what Kotlin writes with the goldens vendored there. This script runs
# protowire-go over the same inputs, so those goldens are shown to be what Go
# writes, and not only a copy of what it once wrote.
#
# Usage: check_go_goldens.sh <protowire checkout> <protowire-go checkout>
# The checkouts come from the pins in .github/workflows/ci.yml.

set -euo pipefail

SPEC_DIR="$(cd "$1" && pwd)"
GO_DIR="$(cd "$2" && pwd)"
VENDORED="$(cd "$(dirname "$0")/../src/test/resources/text" && pwd)"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
(cd "$GO_DIR" && go build -o "$work/check-decode" ./scripts/check_decode)

# check_decode resolves imports from the spec repo's proto/ root, two levels
# above text.proto, so it runs in the spec checkout.
cd "$SPEC_DIR/testdata/text"
cases=0
fails=0
for input in "$VENDORED"/cases/*.binpb; do
  name="$(basename "$input" .binpb)"
  cases=$((cases + 1))
  if ! cmp -s "$input" "cases/$name.binpb"; then
    echo "✗ $name: vendored input differs from the spec repo" >&2
    fails=$((fails + 1))
    continue
  fi
  rc=0
  "$work/check-decode" --format pb-text --schema "text.v1.${name%%.*}" \
    --proto text.proto --input "$input" > "$work/out.pxf" 2> "$work/err" || rc=$?
  if [[ $rc -ne 0 ]] || ! cmp -s "$work/out.pxf" "$VENDORED/cases/$name.expected.pxf"; then
    echo "✗ $name (exit $rc)" >&2
    head -3 "$work/err" >&2
    diff "$VENDORED/cases/$name.expected.pxf" "$work/out.pxf" | head -12 >&2 || true
    fails=$((fails + 1))
  fi
done

# Nothing vendored means nothing compared, which must not read as a pass.
if [[ $cases -eq 0 ]]; then
  echo "no vendored cases under $VENDORED" >&2
  exit 2
fi
upstream="$(find cases -name '*.binpb' | wc -l | tr -d ' ')"
if [[ "$upstream" -ne "$cases" ]]; then
  echo "✗ spec repo has $upstream cases, $cases are vendored" >&2
  fails=$((fails + 1))
fi

echo "Ran $cases cases through protowire-go."
if [[ $fails -ne 0 ]]; then
  echo "✗ $fails cases differ from what protowire-go writes." >&2
  exit 1
fi
echo "✓ protowire-go writes every vendored golden."
