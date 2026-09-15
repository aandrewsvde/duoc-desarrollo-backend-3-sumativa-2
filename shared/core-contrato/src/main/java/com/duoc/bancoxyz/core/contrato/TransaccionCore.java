package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Transaccion diaria del libro del banco. */
public record TransaccionCore(long id, LocalDate fecha, BigDecimal monto, String tipo) {
}
