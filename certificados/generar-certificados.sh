#!/usr/bin/env bash
#
# Genera un almacen de claves PKCS#12 por servicio y un almacen de confianza
# comun, para levantar los cuatro servicios sobre HTTPS.
#
# POR QUE UN CERTIFICADO POR SERVICIO Y NO UNO COMPARTIDO
# Cada BFF es un despliegue independiente con su propio ciclo de vida: rotar el
# certificado de un canal no debe obligar a reiniciar los otros tres. Compartir
# un unico par de claves entre los cuatro servicios los volveria a acoplar
# justo en el punto donde el patron busca separarlos.
#
# POR QUE AUTOFIRMADOS
# Son certificados de desarrollo. En produccion los emitiria una CA corporativa
# o un emisor publico; la configuracion de los servicios no cambia, solo el
# origen del certificado. Se genera ademas una CA propia que firma los cuatro,
# de modo que los clientes confian en UN solo emisor y no en cuatro
# certificados sueltos, que es como funciona una PKI real.
#
set -euo pipefail

DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CLAVE="${CLAVE_ALMACEN:-bancoxyz}"
DIAS=825
SERVICIOS=("core-banking-api" "bff-web" "bff-movil" "bff-cajero")

echo "==> Limpiando certificados anteriores"
rm -f "$DIR"/*.p12 "$DIR"/*.pem "$DIR"/*.srl "$DIR"/*.csr "$DIR"/*.key "$DIR"/*.crt "$DIR"/*.ext

echo "==> Creando la CA de desarrollo del Banco XYZ"
openssl req -x509 -newkey rsa:2048 -sha256 -days $DIAS -nodes \
  -keyout "$DIR/ca-bancoxyz.key" -out "$DIR/ca-bancoxyz.crt" \
  -subj "/C=CL/ST=Santiago/L=Santiago/O=Banco XYZ/OU=Plataforma/CN=Banco XYZ Dev CA" 2>/dev/null

for servicio in "${SERVICIOS[@]}"; do
  echo "==> Emitiendo certificado para $servicio"

  # SAN con localhost y el nombre del servicio: el mismo certificado sirve para
  # ejecucion local y para resolucion por nombre dentro de una red de contenedores.
  cat > "$DIR/$servicio.ext" <<EXT
subjectAltName = DNS:localhost, DNS:$servicio, IP:127.0.0.1
extendedKeyUsage = serverAuth
keyUsage = digitalSignature, keyEncipherment
EXT

  openssl req -newkey rsa:2048 -sha256 -nodes \
    -keyout "$DIR/$servicio.key" -out "$DIR/$servicio.csr" \
    -subj "/C=CL/ST=Santiago/L=Santiago/O=Banco XYZ/OU=Canales/CN=$servicio" 2>/dev/null

  openssl x509 -req -in "$DIR/$servicio.csr" -sha256 -days $DIAS \
    -CA "$DIR/ca-bancoxyz.crt" -CAkey "$DIR/ca-bancoxyz.key" -CAcreateserial \
    -extfile "$DIR/$servicio.ext" -out "$DIR/$servicio.crt" 2>/dev/null

  openssl pkcs12 -export -name "$servicio" \
    -inkey "$DIR/$servicio.key" -in "$DIR/$servicio.crt" -certfile "$DIR/ca-bancoxyz.crt" \
    -out "$DIR/$servicio.p12" -passout "pass:$CLAVE"

  rm -f "$DIR/$servicio.csr" "$DIR/$servicio.ext" "$DIR/$servicio.key" "$DIR/$servicio.crt"
done

echo "==> Construyendo el almacen de confianza (solo la CA)"
keytool -importcert -noprompt -alias ca-bancoxyz \
  -file "$DIR/ca-bancoxyz.crt" \
  -keystore "$DIR/confianza-bancoxyz.p12" -storetype PKCS12 \
  -storepass "$CLAVE" > /dev/null

rm -f "$DIR/ca-bancoxyz.key" "$DIR"/*.srl

echo
echo "Listo. Archivos generados en $DIR:"
ls -1 "$DIR" | grep -E '\.(p12|crt)$' | sed 's/^/  /'
echo
echo "  ca-bancoxyz.crt         CA publica; usala con  curl --cacert"
echo "  confianza-bancoxyz.p12  almacen de confianza para los clientes Java"
echo "  <servicio>.p12          par de claves TLS de cada servicio"
