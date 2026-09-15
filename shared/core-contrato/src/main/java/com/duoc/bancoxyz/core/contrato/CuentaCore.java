package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;

/** Cuenta tal como la publica el core: completa y sin recortar. */
public record CuentaCore(
        int numeroCuenta,
        String titular,
        String tipoCuenta,
        BigDecimal saldo,
        String moneda,
        int edadTitular,
        String estado) {
}
