#!/usr/bin/env bash
# Saves a workflow's history from the dev server as a replay-test fixture.
#   scripts/capture-history.sh order.placed-o-2001 order.placed-success
set -euo pipefail
cd "$(dirname "$0")/.."
workflow_id="${1:?usage: capture-history.sh <workflow-id> <fixture-name>}"
name="${2:?usage: capture-history.sh <workflow-id> <fixture-name>}"
out="testkit/src/main/resources/histories/$name.json"
temporal workflow show --workflow-id "$workflow_id" --output json > "$out"
echo "wrote $out"
