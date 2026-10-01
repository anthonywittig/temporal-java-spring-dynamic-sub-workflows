#!/usr/bin/env bash
# Temporal dev server with the EventType search attribute. UI at http://localhost:8233.
# State persists in .local/temporal.db so captured histories survive restarts.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .local
exec temporal server start-dev \
  --db-filename .local/temporal.db \
  --search-attribute EventType=Keyword
