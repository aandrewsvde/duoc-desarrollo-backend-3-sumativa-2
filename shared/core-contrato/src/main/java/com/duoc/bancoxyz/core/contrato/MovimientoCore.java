package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimiento canonico.
 *
 * @param signo +1 si abona a la cuenta, -1 si carga. El core entrega el signo
 *              ya resuelto para que ningun canal tenga que deducirlo del tipo,
 *              que es justo la clase de logica que el patron BFF busca sacar
 *              del frontend.
 */
public record MovimientoCore(
        long id,
        int cuentaId,
        LocalDate fecha,
        String tipo,
        int signo,
        BigDecimal monto,
        String descripcion) {
}
