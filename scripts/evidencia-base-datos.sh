#!/usr/bin/env bash
# Vuelca el estado de la base que respalda a los tres canales.
set -uo pipefail

Q() { docker exec -i bancoxyz-bff-postgres psql -U bancoxyz -d bancoxyz_bff -P pager=off "$@"; }

printf 'EVIDENCIA DE BASE DE DATOS - core bancario del Banco XYZ\n'
printf 'Generado: %s\n\n' "$(date '+%Y-%m-%d %H:%M:%S')"

echo "### 1. Datos cargados desde bank_legacy_data ###"
Q -c "SELECT 'cuenta' AS tabla, COUNT(*) FROM cuenta
UNION ALL SELECT 'movimiento', COUNT(*) FROM movimiento
UNION ALL SELECT 'transaccion_diaria', COUNT(*) FROM transaccion_diaria
UNION ALL SELECT 'usuario_canal', COUNT(*) FROM usuario_canal
UNION ALL SELECT 'tarjeta', COUNT(*) FROM tarjeta
UNION ALL SELECT 'retiro', COUNT(*) FROM retiro ORDER BY 1;"

echo "### 2. Cuentas: una fila por cuenta, saldo y tipo ###"
Q -c "SELECT tipo_cuenta, COUNT(*) AS cuentas, MIN(saldo) AS saldo_min, MAX(saldo) AS saldo_max
      FROM cuenta GROUP BY tipo_cuenta ORDER BY 1;"
Q -c "SELECT numero_cuenta, titular, tipo_cuenta, saldo, edad_titular, estado
      FROM cuenta ORDER BY numero_cuenta LIMIT 10;"

echo "### 3. Movimientos ya clasificados en abono (+1) y cargo (-1) ###"
Q -c "SELECT tipo, signo, COUNT(*) AS movimientos, SUM(monto) AS monto_total
      FROM movimiento GROUP BY tipo, signo ORDER BY 3 DESC;"

echo "### 4. Credenciales: nunca en claro, siempre BCrypt ###"
Q -c "SELECT usuario, canal, perfil, cuenta_id, LEFT(password_hash, 14) || '...' AS hash_bcrypt
      FROM usuario_canal ORDER BY canal, usuario;"
Q -c "SELECT '**** **** **** ' || RIGHT(numero_tarjeta, 4) AS tarjeta, cuenta_id, activa,
             LEFT(pin_hash, 14) || '...' AS hash_bcrypt
      FROM tarjeta ORDER BY numero_tarjeta;"

echo "### 5. Retiros ejecutados por el canal cajero ###"
Q -c "SELECT cuenta_id, monto, terminal, codigo_autorizacion, saldo_anterior, saldo_resultante,
             LEFT(referencia, 28) || '...' AS referencia
      FROM retiro ORDER BY instante;"

echo "### 6. Cada retiro dejo su movimiento, con el canal que lo origino ###"
Q -c "SELECT cuenta_id, fecha, tipo, signo, monto, descripcion, canal_origen
      FROM movimiento WHERE canal_origen <> 'LEGACY' ORDER BY id;"

echo "### 7. Coherencia: el saldo refleja los retiros del cajero ###"
Q -c "SELECT c.numero_cuenta, c.saldo AS saldo_actual,
             COALESCE(SUM(r.monto), 0) AS retirado_por_cajero,
             c.saldo + COALESCE(SUM(r.monto), 0) AS saldo_antes_de_los_retiros
      FROM cuenta c LEFT JOIN retiro r ON r.cuenta_id = c.numero_cuenta
      WHERE EXISTS (SELECT 1 FROM retiro x WHERE x.cuenta_id = c.numero_cuenta)
      GROUP BY c.numero_cuenta, c.saldo;"
