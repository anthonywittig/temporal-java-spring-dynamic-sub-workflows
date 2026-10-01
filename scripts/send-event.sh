#!/usr/bin/env bash
# Publishes a sample event to the `events` topic, keyed by event ID.
#   scripts/send-event.sh order.placed            # a fixture from testkit/src/main/resources/events
#   scripts/send-event.sh path/to/event.json      # any file
set -euo pipefail
cd "$(dirname "$0")/.."
arg="${1:?usage: send-event.sh <fixture-name|file>}"
file="$arg"
[[ -f "$file" ]] || file="testkit/src/main/resources/events/$arg.json"
json="$(jq -c . "$file")"
key="$(jq -r '.id // "no-id"' <<<"$json")"
printf '%s|%s\n' "$key" "$json" | kafka-console-producer \
  --bootstrap-server "${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}" \
  --topic events \
  --reader-property parse.key=true \
  --reader-property 'key.separator=|'
echo "sent $key: $json"
