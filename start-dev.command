#!/bin/zsh

cd "$(dirname "$0")" || exit 1
exec ./scripts/start-dev.sh
