#!/usr/bin/env bash
#
# Recorrido funcional completo de los tres canales, con la salida de consola que
# se entrega como evidencia de ejecucion.
#
# Cubre lo que pide la pauta: los tres BFF operando, la optimizacion por canal,
# la autenticacion y autorizacion diferenciadas, y las medidas de seguridad.
#
set -uo pipefail

RAIZ="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
CA="$RAIZ/certificados/ca-bancoxyz.crt"
CURL=(curl -s --cacert "$CA")

CORE="https://localhost:8843"
WEB="https://localhost:8443"
MOVIL="https://localhost:8444"
CAJERO="https://localhost:8445"

titulo() { printf '\n========================================================================\n %s\n========================================================================\n' "$1"; }
paso()   { printf '\n--- %s\n' "$1"; }

json() { python3 -m json.tool 2>/dev/null || cat; }

# Muestra el contenido del token sin su firma, para evidenciar emisor,
# audiencia, vigencia y permisos de cada canal.
claims() {
  cut -d. -f2 <<<"$1" | tr '_-' '/+' | base64 -d 2>/dev/null \
    | python3 -c 'import sys,json;d=json.load(sys.stdin);print(json.dumps({k:d[k] for k in ("iss","aud","sub","canal","roles","permisos","cuenta","contexto") if k in d}, indent=2, ensure_ascii=False));print("vigencia:", d["exp"]-d["iat"], "segundos")'
}

codigo() { curl -s -o /dev/null -w '%{http_code}' --cacert "$CA" "$@"; }
tam()    { curl -s -o /dev/null -w '%{size_download}' --cacert "$CA" "$@"; }

printf 'EVIDENCIA DE EJECUCION - Banco XYZ, patron Backend for Frontend\n'
printf 'Generado: %s\n' "$(date '+%Y-%m-%d %H:%M:%S')"

# =====================================================================
titulo "1. HTTPS: los cuatro servicios presentan certificado de la CA del banco"
# =====================================================================
for entrada in "core-banking-api:8843" "bff-web:8443" "bff-movil:8444" "bff-cajero:8445"; do
  nombre="${entrada%%:*}"; puerto="${entrada##*:}"
  sujeto=$(echo | openssl s_client -connect "localhost:$puerto" -CAfile "$CA" 2>/dev/null \
           | openssl x509 -noout -subject -issuer 2>/dev/null | tr '\n' ' ')
  verificacion=$(echo | openssl s_client -connect "localhost:$puerto" -CAfile "$CA" 2>/dev/null \
           | grep -m1 "Verify return code")
  printf '  %-18s puerto %s\n      %s\n      %s\n' "$nombre" "$puerto" "$sujeto" "$verificacion"
done

paso "Una peticion que no confia en la CA del banco es rechazada por el cliente"
curl -sS "$WEB/api/web/auth/login" 2>&1 | head -3 | sed 's/^/  /'
echo "  Los certificados los firma una CA propia; el cliente debe confiar en ella"
echo "  de forma explicita (--cacert), que es como opera una PKI corporativa."

# =====================================================================
titulo "2. AUTENTICACION: cada canal emite su propio token"
# =====================================================================
paso "Canal WEB: usuario y contrasena"
RESP_WEB=$("${CURL[@]}" -X POST "$WEB/api/web/auth/login" -H 'Content-Type: application/json' \
  -d '{"usuario":"cliente101","password":"Web2024$101"}')
echo "$RESP_WEB" | json
TOKEN_WEB=$(python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])' <<<"$RESP_WEB")
paso "Contenido del token WEB"
claims "$TOKEN_WEB"

paso "Canal MOVIL: usuario, contrasena e identificador de dispositivo"
RESP_MOV=$("${CURL[@]}" -X POST "$MOVIL/api/movil/auth/login" -H 'Content-Type: application/json' \
  -d '{"usuario":"movil101","password":"Movil2024$101","dispositivoId":"pixel-8-abc123"}')
echo "$RESP_MOV" | json
TOKEN_MOV=$(python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])' <<<"$RESP_MOV")
paso "Contenido del token MOVIL (vigencia mas corta y ligado al dispositivo)"
claims "$TOKEN_MOV"

paso "Canal CAJERO: tarjeta y PIN"
RESP_ATM=$("${CURL[@]}" -X POST "$CAJERO/api/cajero/auth/pin" -H 'Content-Type: application/json' \
  -d '{"numeroTarjeta":"4051885600000101","pin":"1101","terminal":"ATM-PROVIDENCIA-07"}')
echo "$RESP_ATM" | json
TOKEN_ATM=$(python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])' <<<"$RESP_ATM")
paso "Contenido del token CAJERO (120 segundos y ligado al terminal)"
claims "$TOKEN_ATM"

paso "Credenciales incorrectas: mismo mensaje generico, sin revelar si existe el usuario"
"${CURL[@]}" -X POST "$WEB/api/web/auth/login" -H 'Content-Type: application/json' \
  -d '{"usuario":"cliente101","password":"incorrecta"}' | json
"${CURL[@]}" -X POST "$CAJERO/api/cajero/auth/pin" -H 'Content-Type: application/json' \
  -d '{"numeroTarjeta":"4051885600000101","pin":"9999","terminal":"ATM-PROVIDENCIA-07"}' | json

# =====================================================================
titulo "3. CANAL WEB: respuesta completa y compuesta"
# =====================================================================
paso "GET /api/web/cuentas/101/panel  (una sola llamada)"
"${CURL[@]}" "$WEB/api/web/cuentas/101/panel" -H "Authorization: Bearer $TOKEN_WEB" | json
paso "GET /api/web/cuentas/101/movimientos?pagina=0&tamano=3"
"${CURL[@]}" "$WEB/api/web/cuentas/101/movimientos?pagina=0&tamano=3" -H "Authorization: Bearer $TOKEN_WEB" | json

# =====================================================================
titulo "4. CANAL MOVIL: la misma cuenta, respuesta minima"
# =====================================================================
paso "GET /api/movil/resumen  (sin numero de cuenta: sale del token)"
"${CURL[@]}" "$MOVIL/api/movil/resumen" -H "Authorization: Bearer $TOKEN_MOV"
printf '\n\n  bytes del resumen movil: %s\n' "$(tam "$MOVIL/api/movil/resumen" -H "Authorization: Bearer $TOKEN_MOV")"
printf '  bytes del panel web:     %s\n' "$(tam "$WEB/api/web/cuentas/101/panel" -H "Authorization: Bearer $TOKEN_WEB")"

paso "GET /api/movil/movimientos?limite=3"
"${CURL[@]}" "$MOVIL/api/movil/movimientos?limite=3" -H "Authorization: Bearer $TOKEN_MOV"; echo

paso "El canal acota lo que la app puede pedir (limite 50 sobre un maximo de 20)"
"${CURL[@]}" "$MOVIL/api/movil/movimientos?limite=50" -H "Authorization: Bearer $TOKEN_MOV" | json

# =====================================================================
titulo "5. CANAL CAJERO: operaciones criticas"
# =====================================================================
paso "GET /api/cajero/saldo"
"${CURL[@]}" "$CAJERO/api/cajero/saldo" -H "Authorization: Bearer $TOKEN_ATM"; echo

paso "POST /api/cajero/retiro  monto 2000"
"${CURL[@]}" -X POST "$CAJERO/api/cajero/retiro" -H "Authorization: Bearer $TOKEN_ATM" \
  -H 'Content-Type: application/json' -d '{"monto":2000}' | json

paso "Saldo despues del retiro"
"${CURL[@]}" "$CAJERO/api/cajero/saldo" -H "Authorization: Bearer $TOKEN_ATM"; echo

paso "Reglas del canal: monto que no es multiplo de 1000"
"${CURL[@]}" -X POST "$CAJERO/api/cajero/retiro" -H "Authorization: Bearer $TOKEN_ATM" \
  -H 'Content-Type: application/json' -d '{"monto":1500}' | json

paso "Reglas del canal: monto sobre el maximo por operacion"
"${CURL[@]}" -X POST "$CAJERO/api/cajero/retiro" -H "Authorization: Bearer $TOKEN_ATM" \
  -H 'Content-Type: application/json' -d '{"monto":500000}' | json

paso "Saldo insuficiente"
"${CURL[@]}" -X POST "$CAJERO/api/cajero/retiro" -H "Authorization: Bearer $TOKEN_ATM" \
  -H 'Content-Type: application/json' -d '{"monto":100000}' | json

# =====================================================================
titulo "6. AUTORIZACION: un token solo sirve en su canal"
# =====================================================================
printf '\n  %-12s %-14s %-14s %s\n' "TOKEN" "-> bff-web" "-> bff-movil" "-> bff-cajero"
for par in "WEB:$TOKEN_WEB" "MOVIL:$TOKEN_MOV" "CAJERO:$TOKEN_ATM"; do
  etq="${par%%:*}"; tok="${par#*:}"
  c1=$(codigo "$WEB/api/web/cuentas/101/panel" -H "Authorization: Bearer $tok")
  c2=$(codigo "$MOVIL/api/movil/resumen" -H "Authorization: Bearer $tok")
  c3=$(codigo "$CAJERO/api/cajero/saldo" -H "Authorization: Bearer $tok")
  printf '  %-12s %-14s %-14s %s\n' "$etq" "HTTP $c1" "HTTP $c2" "HTTP $c3"
done
echo
echo "  Cada canal firma con una clave distinta y declara su propia audiencia:"
echo "  la firma de un token ajeno ni siquiera verifica."

paso "Detalle del rechazo (token movil presentado al canal cajero)"
"${CURL[@]}" "$CAJERO/api/cajero/saldo" -H "Authorization: Bearer $TOKEN_MOV" | json

paso "Sin token"
"${CURL[@]}" "$WEB/api/web/cuentas/101/panel" | json

# =====================================================================
titulo "7. AUTORIZACION A NIVEL DE RECURSO: la cuenta ajena"
# =====================================================================
paso "cliente101 intenta ver el panel de la cuenta 102"
"${CURL[@]}" "$WEB/api/web/cuentas/102/panel" -H "Authorization: Bearer $TOKEN_WEB" | json

paso "El ejecutivo si puede ver cualquier cuenta"
TOKEN_EJE=$("${CURL[@]}" -X POST "$WEB/api/web/auth/login" -H 'Content-Type: application/json' \
  -d '{"usuario":"ejecutivo","password":"Ejecutivo2024$"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
echo "  permisos del ejecutivo:"
claims "$TOKEN_EJE" | head -20
printf '  panel de la cuenta 102 con token de ejecutivo: HTTP %s\n' \
  "$(codigo "$WEB/api/web/cuentas/102/panel" -H "Authorization: Bearer $TOKEN_EJE")"

# =====================================================================
titulo "8. DEFENSA EN PROFUNDIDAD: el core autoriza por canal"
# =====================================================================
echo "  El core reconoce permisos distintos a cada BFF. Se emiten tokens de"
echo "  servicio validos para cada uno y se prueban contra el mismo endpoint."
echo
python3 - "$CA" <<'PYEOF'
import base64, hashlib, hmac, http.client, json, ssl, sys, time

CA = sys.argv[1]
SECRETO = "core-bancoxyz-secreto-de-servicio-2026-min-32-bytes"
contexto = ssl.create_default_context(cafile=CA)

def b64(d): return base64.urlsafe_b64encode(d).rstrip(b"=").decode()

def token(cliente):
    ahora = int(time.time())
    payload = {"iss": "banco-xyz-canales", "aud": ["core-banking-api"], "sub": cliente,
               "canal": "SERVICIO", "roles": ["ROLE_SERVICIO"], "permisos": ["LEER_CORE"],
               "contexto": cliente, "iat": ahora, "exp": ahora + 300}
    cab = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    cue = b64(json.dumps(payload, separators=(",", ":")).encode())
    firma = hmac.new(SECRETO.encode(), f"{cab}.{cue}".encode(), hashlib.sha256).digest()
    return f"{cab}.{cue}.{b64(firma)}"

def probar(cliente, metodo, ruta, cuerpo=None):
    c = http.client.HTTPSConnection("localhost", 8843, context=contexto)
    datos = json.dumps(cuerpo).encode() if cuerpo else None
    cab = {"Authorization": f"Bearer {token(cliente)}"}
    if datos: cab["Content-Type"] = "application/json"
    c.request(metodo, ruta, body=datos, headers=cab)
    return c.getresponse().status

pruebas = [
    ("GET  /api/v1/cuentas/101",              "GET",  "/api/v1/cuentas/101", None),
    ("GET  /api/v1/cuentas/101/resumen-anual","GET",  "/api/v1/cuentas/101/resumen-anual?anio=2024", None),
    ("POST /api/v1/cuentas/101/retiros",      "POST", "/api/v1/cuentas/101/retiros",
        {"monto": 1000, "terminal": "X", "referencia": "prueba-permisos"}),
    ("POST /api/v1/identidades/validar-tarjeta","POST","/api/v1/identidades/validar-tarjeta",
        {"numeroTarjeta": "4051885600000101", "pin": "0000"}),
]

print(f"  {'OPERACION':<42} {'bff-web':>9} {'bff-movil':>11} {'bff-cajero':>12}")
for etiqueta, metodo, ruta, cuerpo in pruebas:
    fila = [probar(c, metodo, ruta, cuerpo) for c in ("bff-web", "bff-movil", "bff-cajero")]
    print(f"  {etiqueta:<42} {fila[0]:>9} {fila[1]:>11} {fila[2]:>12}")
print()
print("  403 = el core rechaza la operacion para ese canal aunque el token sea valido.")
print("  El BFF movil no puede retirar dinero ni aunque su propio codigo lo intentara.")
PYEOF

titulo "FIN DE LA EVIDENCIA"
