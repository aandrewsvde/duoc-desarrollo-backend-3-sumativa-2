package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;

/** Cantidad e importe acumulado por tipo de movimiento. */
public record ConteoTipoCore(String tipo, int cantidad, BigDecimal monto) {
}
