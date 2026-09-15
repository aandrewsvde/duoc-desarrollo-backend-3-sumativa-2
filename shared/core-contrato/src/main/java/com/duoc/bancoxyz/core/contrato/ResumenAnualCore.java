package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Agregados anuales de una cuenta, calculados en el motor de base de datos. */
public record ResumenAnualCore(
        int cuentaId,
        int anio,
        int totalMovimientos,
        BigDecimal totalAbonos,
        BigDecimal totalCargos,
        BigDecimal netoPeriodo,
        LocalDate primerMovimiento,
        LocalDate ultimoMovimiento,
        List<ConteoTipoCore> porTipo,
        List<TotalMensualCore> porMes) {
}
