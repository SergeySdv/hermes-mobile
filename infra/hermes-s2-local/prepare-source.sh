#!/bin/sh
set -eu
source_dir=${1:?Usage: sh prepare-source.sh /path/to/hermes-agent-checkout}
expected=4d04303818b61bf3aa09278af48356315515965b
actual=$(git -C "$source_dir" rev-parse HEAD)
[ "$actual" = "$expected" ] || { echo "Wrong Hermes source SHA" >&2; exit 1; }
cd "$(dirname "$0")"
mkdir -p .local
chmod 700 .local
git -C "$source_dir" archive "$expected" -o "$PWD/.local/source.tar"
echo "Archived pinned Hermes source $expected (working-tree changes excluded)"
