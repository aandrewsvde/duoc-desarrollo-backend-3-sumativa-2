#!/usr/bin/env bash
#
# Levanta la plataforma completa: PostgreSQL, el core y los tres BFF.
#
# Cada servicio se arranca desde su propio directorio de modulo porque los
# almacenes de claves se referencian como ../certificados/, de modo que el
# directorio de trabajo forma parte de la configuracion.
#
set -euo pipefail

RAIZ="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS="$RAIZ/evidencia/logs"
mkdir -p "$LOGS"

servicios=(
  "core-banking-api:8843"
  "bff-web:8443"
  "bff-movil:8444"
  "bff-cajero:8445"
)

echo "==> Levantando PostgreSQL"
docker compose -f "$RAIZ/docker-compose.yml" up -d >/dev/null
until docker exec bancoxyz-bff-postgres pg_isready -U bancoxyz -d bancoxyz_bff >/dev/null 2>&1; do
  sleep 1
done
echo "    PostgreSQL disponible en el puerto 5434"

for entrada in "${servicios[@]}"; do
  modulo="${entrada%%:*}"
  puerto="${entrada##*:}"
  echo "==> Arrancando $modulo en el puerto $puerto"
  (
    cd "$RAIZ/$modulo"
    nohup java -jar "target/$modulo-1.0.0.jar" > "$LOGS/$modulo.log" 2>&1 &
    echo $! > "$RAIZ/scripts/.$modulo.pid"
  )
done

echo "==> Esperando a que los servicios respondan"
for entrada in "${servicios[@]}"; do
  modulo="${entrada%%:*}"
  puerto="${entrada##*:}"
  intentos=0
  # El core arranca primero porque los BFF no sirven de nada sin el, pero no se
  # bloquea la partida de los demas: cada uno reintenta contra su propio puerto.
  until curl -fsS --cacert "$RAIZ/certificados/ca-bancoxyz.crt" \
        "https://localhost:$puerto/actuator/health" >/dev/null 2>&1 \
     || curl -fsS --cacert "$RAIZ/certificados/ca-bancoxyz.crt" \
        "https://localhost:$puerto/swagger-ui.html" >/dev/null 2>&1; do
    intentos=$((intentos + 1))
    if [ "$intentos" -gt 60 ]; then
      echo "    $modulo NO respondio. Ultimas lineas de su log:"
      tail -25 "$LOGS/$modulo.log" | sed 's/^/      /'
      exit 1
    fi
    sleep 1
  done
  echo "    $modulo listo -> https://localhost:$puerto"
done

echo
echo "Plataforma arriba:"
echo "  core-banking-api  https://localhost:8843/swagger-ui.html   (solo servicios)"
echo "  bff-web           https://localhost:8443/swagger-ui.html"
echo "  bff-movil         https://localhost:8444/swagger-ui.html"
echo "  bff-cajero        https://localhost:8445/swagger-ui.html"
echo
echo "Los clientes deben confiar en la CA: --cacert certificados/ca-bancoxyz.crt"
