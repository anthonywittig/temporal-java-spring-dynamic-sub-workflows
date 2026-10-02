#!/usr/bin/env bash
# Single-node Kafka (KRaft) on localhost:9092 without Docker. Needs `brew install kafka`.
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$PWD/.local/kafka"
mkdir -p "$dir"
config="$dir/server.properties"
sed "s|^log.dirs=.*|log.dirs=$dir/data|" "$(brew --prefix)/etc/kafka/server.properties" > "$config"
if [[ ! -f "$dir/data/meta.properties" ]]; then
  kafka-storage format --standalone -t "$(kafka-storage random-uuid)" -c "$config"
fi
exec kafka-server-start "$config"
