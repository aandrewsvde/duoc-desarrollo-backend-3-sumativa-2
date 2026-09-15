package com.duoc.bancoxyz.bff.web.panel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totales del ejercicio.
 *
 * <p>El campo {@code netoPeriodo} es el neto de los movimientos del ano y no
 * tiene por que coincidir con el saldo: son dos cifras distintas y se publican
 * por separado para que la interfaz no las confunda.</p>
 */
public record ResumenAnualWeb(
        int anio,
        int totalMovimientos,
        BigDecimal totalAbonos,
        String totalAbonosFormateado,
        BigDecimal totalCargos,
        String totalCargosFormateado,
        BigDecimal netoPeriodo,
        String netoPeriodoFormateado,
        LocalDate primerMovimiento,
        LocalDate ultimoMovimiento) {
}
