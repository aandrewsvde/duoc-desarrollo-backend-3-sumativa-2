package com.duoc.bancoxyz.bff.web.panel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Movimiento con todo lo que la tabla del navegador necesita mostrar. */
public record MovimientoWeb(
        long id,
        LocalDate fecha,
        String fechaFormateada,
        String tipo,
        String etiquetaTipo,
        String descripcion,
        BigDecimal monto,
        String montoFormateado,
        int signo,
        String naturaleza) {
}
