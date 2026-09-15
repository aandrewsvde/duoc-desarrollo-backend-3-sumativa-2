package com.duoc.bancoxyz.bff.movil.autenticacion;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales del canal movil.
 *
 * <p>Exige ademas el identificador del dispositivo, que el canal web no pide.
 * Queda grabado dentro del token y en el log, de modo que una sesion siempre
 * puede asociarse al aparato desde el que se abrio.</p>
 */
public record SolicitudLoginMovil(
        @NotBlank(message = "el usuario es obligatorio") String usuario,
        @NotBlank(message = "la contrasena es obligatoria") String password,
        @NotBlank(message = "el identificador del dispositivo es obligatorio") String dispositivoId) {
}
