package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;

/** Abonos y cargos acumulados de un mes, en formato yyyy-MM. */
public record TotalMensualCore(String mes, BigDecimal abonos, BigDecimal cargos) {
}
