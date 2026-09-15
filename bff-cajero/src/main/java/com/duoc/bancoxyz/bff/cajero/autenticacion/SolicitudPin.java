package com.duoc.bancoxyz.bff.cajero.autenticacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Credenciales del cajero: tarjeta, PIN y terminal.
 *
 * <p>El identificador del terminal es obligatorio y queda dentro del token. Sin
 * el, un retiro no podria atribuirse al cajero fisico que entrego el dinero, que
 * es el primer dato que se busca cuando un cliente reclama.</p>
 */
public record SolicitudPin(
        @NotBlank(message = "el numero de tarjeta es obligatorio")
        @Pattern(regexp = "\\d{16}", message = "el numero de tarjeta debe tener 16 digitos")
        String numeroTarjeta,

        @NotBlank(message = "el PIN es obligatorio")
        @Pattern(regexp = "\\d{4}", message = "el PIN debe tener 4 digitos")
        String pin,

        @NotBlank(message = "el identificador del terminal es obligatorio")
        String terminal) {
}
