#!/usr/bin/env python3
"""
Genera el archivo de semilla del core-banking-api a partir de los CSV legacy
del Banco XYZ (https://github.com/KariVillagran/bank_legacy_data).

Aplica exactamente las mismas reglas de saneamiento que el proceso batch de la
Experiencia 1, de modo que la API de esta experiencia sirve el mismo conjunto de
datos limpios que aquel proceso habria dejado en la base:

  - fechas normalizadas a ISO-8601 desde los cuatro formatos que conviven en el
    archivo, con rechazo estricto de fechas imposibles (2024-13-01);
  - montos ausentes, cero o negativos descartados: el signo contable lo define
    el tipo de operacion, no el importe grabado por el sistema antiguo;
  - tipos canonizados sin acentos ni mayusculas, para unificar las variantes que
    el legacy grabo distinto ("deposito" y "deposito" con tilde);
  - catalogos cerrados: se descartan los marcadores de relleno (-1, unknown).

Uso:  python3 scripts/generar-semilla.py <dir_csv> <archivo_sql_salida>
"""
import csv
import datetime
import sys
import unicodedata
from pathlib import Path

FORMATOS_FECHA = ["%Y-%m-%d", "%d-%m-%Y", "%d/%m/%Y", "%Y/%m/%d"]
TIPOS_CUENTA = {"ahorro", "prestamo", "hipoteca"}
TIPOS_MOVIMIENTO = {"deposito", "retiro", "compra", "pago"}
ABONOS = {"deposito"}
TIPOS_TRANSACCION = {"credito", "debito"}
ANIO_EJERCICIO = 2024
EDAD_MINIMA, EDAD_MAXIMA = 18, 100


def canonico(valor: str) -> str:
    """Minusculas, sin espacios sobrantes y sin acentos."""
    sin_acentos = unicodedata.normalize("NFD", (valor or "").strip())
    sin_acentos = sin_acentos.encode("ascii", "ignore").decode("ascii")
    return sin_acentos.lower()


def parsear_fecha(valor: str):
    """Devuelve la fecha o None. Estricto: rechaza dias inexistentes."""
    limpio = (valor or "").strip()
    for formato in FORMATOS_FECHA:
        try:
            return datetime.datetime.strptime(limpio, formato).date()
        except ValueError:
            continue
    return None


def escapar(texto: str) -> str:
    return (texto or "").replace("'", "''")


def leer(ruta: Path):
    with ruta.open(newline="", encoding="utf-8") as fh:
        return list(csv.DictReader(fh))


def construir_cuentas(filas):
    """
    Una fila por cuenta: la primera aparicion que supera todas las validaciones.
    El archivo trae ~20 filas por cuenta con datos contradictorios entre si, de
    modo que quedarse con la primera valida es un criterio determinista y
    reproducible.
    """
    cuentas, descartadas = {}, 0
    for fila in filas:
        numero = fila["cuenta_id"].strip()
        if numero in cuentas:
            continue
        saldo = fila["saldo"].strip()
        edad = fila["edad"].strip()
        tipo = canonico(fila["tipo"])
        nombre = fila["nombre"].strip()
        if not saldo or float(saldo) <= 0:
            descartadas += 1; continue
        if not edad.isdigit() or not (EDAD_MINIMA <= int(edad) <= EDAD_MAXIMA):
            descartadas += 1; continue
        if tipo not in TIPOS_CUENTA:
            descartadas += 1; continue
        if canonico(nombre) in ("", "unknown"):
            descartadas += 1; continue
        cuentas[numero] = {
            "numero": int(numero),
            "titular": nombre,
            "tipo": tipo.upper(),
            "saldo": round(float(saldo), 2),
            "edad": int(edad),
        }
    return cuentas, descartadas


def construir_movimientos(filas, cuentas_validas):
    movimientos, descartados = [], 0
    for fila in filas:
        numero = fila["cuenta_id"].strip()
        monto = fila["monto"].strip()
        fecha = parsear_fecha(fila["fecha"])
        tipo = canonico(fila["transaccion"])
        if numero not in cuentas_validas:
            descartados += 1; continue
        if not monto or float(monto) <= 0:
            descartados += 1; continue
        if fecha is None or fecha.year != ANIO_EJERCICIO:
            descartados += 1; continue
        if tipo not in TIPOS_MOVIMIENTO:
            descartados += 1; continue
        descripcion = fila["descripcion"].strip() or "SIN DESCRIPCION"
        movimientos.append({
            "cuenta": int(numero),
            "fecha": fecha.isoformat(),
            "tipo": tipo.upper(),
            "signo": 1 if tipo in ABONOS else -1,
            "monto": round(float(monto), 2),
            "descripcion": descripcion,
        })
        descartados += 0
    movimientos.sort(key=lambda m: (m["cuenta"], m["fecha"]))
    return movimientos, descartados


def construir_transacciones(filas):
    transacciones, descartadas = [], 0
    for fila in filas:
        monto = fila["monto"].strip()
        fecha = parsear_fecha(fila["fecha"])
        tipo = canonico(fila["tipo"])
        if not monto or float(monto) <= 0:
            descartadas += 1; continue
        if fecha is None or fecha.year != ANIO_EJERCICIO:
            descartadas += 1; continue
        if tipo not in TIPOS_TRANSACCION:
            descartadas += 1; continue
        transacciones.append({
            "id": int(fila["id"]),
            "fecha": fecha.isoformat(),
            "monto": round(float(monto), 2),
            "tipo": tipo.upper(),
        })
    transacciones.sort(key=lambda t: t["id"])
    return transacciones, descartadas


def main():
    origen = Path(sys.argv[1])
    destino = Path(sys.argv[2])

    cuentas, cuentas_desc = construir_cuentas(leer(origen / "intereses.csv"))
    movimientos, movs_desc = construir_movimientos(
        leer(origen / "cuentas_anuales.csv"), set(cuentas))
    transacciones, tx_desc = construir_transacciones(leer(origen / "transacciones.csv"))

    lineas = [
        "-- ARCHIVO GENERADO. No editar a mano.",
        "-- Fuente: https://github.com/KariVillagran/bank_legacy_data (data/semana_3)",
        "-- Generador: scripts/generar-semilla.py",
        f"-- Generado: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        "--",
        f"-- cuentas:       {len(cuentas):>4} de 1000 filas de intereses.csv "
        f"({cuentas_desc} filas descartadas antes de completar las 50 cuentas)",
        f"-- movimientos:   {len(movimientos):>4} de 1000 filas de cuentas_anuales.csv",
        f"-- transacciones: {len(transacciones):>4} de 1000 filas de transacciones.csv",
        "",
        "-- ------------------------------------------------------------------",
        "-- Cuentas (titular, tipo y saldo de apertura)",
        "-- ------------------------------------------------------------------",
        "INSERT INTO cuenta (numero_cuenta, titular, tipo_cuenta, saldo, edad_titular, estado) VALUES",
    ]
    filas_sql = [
        f"  ({c['numero']}, '{escapar(c['titular'])}', '{c['tipo']}', {c['saldo']:.2f}, {c['edad']}, 'ACTIVA')"
        for c in sorted(cuentas.values(), key=lambda c: c["numero"])
    ]
    lineas.append(",\n".join(filas_sql) + ";")
    lineas += [
        "",
        "-- ------------------------------------------------------------------",
        "-- Movimientos del ejercicio 2024, ya clasificados en abono (+1) y cargo (-1)",
        "-- ------------------------------------------------------------------",
        "INSERT INTO movimiento (cuenta_id, fecha, tipo, signo, monto, descripcion) VALUES",
    ]
    filas_sql = [
        f"  ({m['cuenta']}, DATE '{m['fecha']}', '{m['tipo']}', {m['signo']}, "
        f"{m['monto']:.2f}, '{escapar(m['descripcion'])}')"
        for m in movimientos
    ]
    lineas.append(",\n".join(filas_sql) + ";")
    lineas += [
        "",
        "-- ------------------------------------------------------------------",
        "-- Transacciones diarias validadas",
        "-- ------------------------------------------------------------------",
        "INSERT INTO transaccion_diaria (id, fecha, monto, tipo) VALUES",
    ]
    filas_sql = [
        f"  ({t['id']}, DATE '{t['fecha']}', {t['monto']:.2f}, '{t['tipo']}')"
        for t in transacciones
    ]
    lineas.append(",\n".join(filas_sql) + ";")
    lineas += [
        "",
        "-- NOTA SOBRE EL SALDO",
        "-- El saldo vigente proviene de intereses.csv y NO se recalcula desde los",
        "-- movimientos de cuentas_anuales.csv: son dos fuentes legacy distintas que",
        "-- no reconcilian entre si (los cargos del historial superan a los abonos, de",
        "-- modo que recalcular dejaria en negativo a cuentas de ahorro). Se conserva",
        "-- cada fuente con su significado: intereses.csv da el saldo actual y",
        "-- cuentas_anuales.csv el historial del ejercicio. Las operaciones que la API",
        "-- ejecuta a partir de ahora -un retiro por cajero- si actualizan el saldo y",
        "-- registran su movimiento, de modo que ambos quedan consistentes hacia",
        "-- adelante. El desajuste historico se reporta como dato, no se oculta:",
        "-- el resumen anual expone el neto del periodo como cifra separada del saldo.",
        "",
        "SELECT setval(pg_get_serial_sequence('movimiento', 'id'), COALESCE(MAX(id), 1)) FROM movimiento;",
        "",
    ]

    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text("\n".join(lineas), encoding="utf-8")

    print(f"cuentas:       {len(cuentas)}")
    print(f"movimientos:   {len(movimientos)}  (descartados {movs_desc})")
    print(f"transacciones: {len(transacciones)}  (descartadas {tx_desc})")
    print(f"escrito en:    {destino}")


if __name__ == "__main__":
    main()
