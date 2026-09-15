package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Resultado de un retiro, aprobado o rechazado con su motivo. */
public record ResultadoRetiroCore(
        boolean aprobado,
        int cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoAnterior,
        BigDecimal saldoResultante,
        String codigoAutorizacion,
        OffsetDateTime instante,
        String motivoRechazo) {
}
