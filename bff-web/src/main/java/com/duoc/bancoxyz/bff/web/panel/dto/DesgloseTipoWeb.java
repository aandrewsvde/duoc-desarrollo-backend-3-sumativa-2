package com.duoc.bancoxyz.bff.web.panel.dto;

import java.math.BigDecimal;

/** Participacion de cada tipo de operacion, lista para un grafico de torta. */
public record DesgloseTipoWeb(
        String tipo,
        String etiqueta,
        int cantidad,
        BigDecimal monto,
        String montoFormateado,
        BigDecimal porcentaje) {
}
