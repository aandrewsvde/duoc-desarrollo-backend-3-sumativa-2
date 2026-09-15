package com.duoc.bancoxyz.bff.web.panel.dto;

import java.math.BigDecimal;

/** Punto de la serie mensual, listo para un grafico de barras. */
public record SerieMensualWeb(
        String mes,
        String etiquetaMes,
        BigDecimal abonos,
        BigDecimal cargos,
        BigDecimal neto) {
}
