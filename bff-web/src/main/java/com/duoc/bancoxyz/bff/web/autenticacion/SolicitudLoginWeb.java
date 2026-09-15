package com.duoc.bancoxyz.bff.web.autenticacion;

import jakarta.validation.constraints.NotBlank;

/** Credenciales del canal web. */
public record SolicitudLoginWeb(
        @NotBlank(message = "el usuario es obligatorio") String usuario,
        @NotBlank(message = "la contrasena es obligatoria") String password) {
}
