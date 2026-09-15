package com.duoc.bancoxyz.core.contrato;

import java.math.BigDecimal;

/**
 * Orden de retiro enviada al core.
 *
 * @param referencia identificador unico de la operacion generado por el canal.
 *                   El core lo usa para descartar reintentos duplicados: si un
 *                   cajero reenvia la misma orden por un corte de red, el
 *                   dinero se descuenta una sola vez.
 */
public record SolicitudRetiroCore(BigDecimal monto, String terminal, String referencia) {
}
