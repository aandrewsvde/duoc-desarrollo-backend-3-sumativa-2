package com.duoc.bancoxyz.bff.cajero.operacion.dto;

/**
 * Comprobante que el cajero imprime.
 *
 * <p>Contiene exactamente los datos de un voucher: que se entrego, que queda, el
 * codigo de autorizacion para un reclamo posterior y el terminal que lo emitio.</p>
 */
public record ComprobanteRetiro(
        boolean aprobado,
        long montoEntregado,
        long saldoRestante,
        String moneda,
        String codigoAutorizacion,
        String terminal,
        String instante,
        String motivoRechazo) {
}
