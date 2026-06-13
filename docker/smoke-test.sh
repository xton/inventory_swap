#!/usr/bin/env bash
# Smoke-tests the InventorySwap plugin against a real Paper server in Docker:
# builds the jar, boots a Paper container with it installed, and checks that
# the server starts up and the plugin enables cleanly.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Building plugin jar"
./gradlew jar

rm -rf docker/plugins
mkdir -p docker/plugins
cp build/libs/*.jar docker/plugins/

cd docker
trap 'docker compose down -v' EXIT

echo "==> Starting Paper server"
docker compose up -d

echo "==> Waiting for server to finish starting"
READY=false
for _ in $(seq 1 60); do
  if docker compose logs paper 2>/dev/null | grep -q "Done ("; then
    READY=true
    break
  fi
  sleep 5
done

if [ "$READY" != "true" ]; then
  echo "Server did not finish starting in time" >&2
  docker compose logs paper
  exit 1
fi

LOGS=$(docker compose logs paper)

if echo "$LOGS" | grep -qi "Error occurred while enabling InventorySwap"; then
  echo "Plugin failed to enable!" >&2
  echo "$LOGS"
  exit 1
fi

if ! echo "$LOGS" | grep -q "\[InventorySwap\] Enabling InventorySwap"; then
  echo "Did not find InventorySwap enable message in server logs" >&2
  echo "$LOGS"
  exit 1
fi

echo "==> Verifying plugin is loaded via RCON"
PLUGINS_OUTPUT=$(docker compose exec -T paper rcon-cli plugins)
echo "$PLUGINS_OUTPUT"

if ! echo "$PLUGINS_OUTPUT" | grep -q "InventorySwap"; then
  echo "InventorySwap not listed by /plugins" >&2
  exit 1
fi

echo "==> Smoke test passed"
