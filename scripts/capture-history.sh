#!/usr/bin/env bash
# Saves a workflow's history from the dev server as a replay-test fixture.
#   scripts/capture-history.sh order.placed-o-2001 order.placed-success
set -euo pipefail
cd "$(dirname "$0")/.."
workflow_id="${1:?usage: capture-history.sh <workflow-id> <fixture-name>}"
name="${2:?usage: capture-history.sh <workflow-id> <fixture-name>}"
out="testkit/src/main/resources/histories/$name.json"
# Worker identities and sticky queue names contain pid@host; scrub them so fixtures don't leak
# machine details.
temporal workflow show --workflow-id "$workflow_id" --output json \
  | jq 'walk(if type == "object" and has("identity") then .identity = "poc" else . end)
         | walk(if type == "string" then gsub("[0-9]+@[A-Za-z0-9.-]+"; "poc") else . end)' > "$out"
echo "wrote $out"
