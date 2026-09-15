#!/usr/bin/env bash
# Detiene los cuatro servicios y, con --todo, tambien PostgreSQL.
set -uo pipefail

RAIZ="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

for modulo in core-banking-api bff-web bff-movil bff-cajero; do
  archivo="$RAIZ/scripts/.$modulo.pid"
  if [ -f "$archivo" ]; then
    pid=$(cat "$archivo")
    if kill "$pid" 2>/dev/null; then
      echo "Detenido $modulo (pid $pid)"
    fi
    rm -f "$archivo"
  fi
done

if [ "${1:-}" = "--todo" ]; then
  docker compose -f "$RAIZ/docker-compose.yml" down >/dev/null 2>&1
  echo "PostgreSQL detenido"
fi
