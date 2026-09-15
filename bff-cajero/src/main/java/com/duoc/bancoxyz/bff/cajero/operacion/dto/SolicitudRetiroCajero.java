package com.duoc.bancoxyz.bff.cajero.operacion.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Orden de retiro.
 *
 * <p>Solo lleva el monto. La cuenta viene del token y el terminal tambien: si
 * cualquiera de los dos viajara en el cuerpo, un cliente manipulado podria
 * retirar de una cuenta ajena o atribuir la operacion a otro cajero.</p>
 */
public record SolicitudRetiroCajero(
        @NotNull(message = "el monto es obligatorio") BigDecimal monto) {
}
