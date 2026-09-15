package com.duoc.bancoxyz.bff.movil.autenticacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticacion del canal movil. */
@RestController
@RequestMapping("/api/movil/auth")
@Tag(name = "Autenticacion movil", description = "Apertura de sesion ligada a un dispositivo")
public class AutenticacionMovilControlador {

    private final ServicioAutenticacionMovil servicio;

    public AutenticacionMovilControlador(ServicioAutenticacionMovil servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    @Operation(summary = "Abre una sesion movil y devuelve un token del canal MOVIL")
    public RespuestaLoginMovil login(@Valid @RequestBody SolicitudLoginMovil solicitud) {
        return servicio.autenticar(solicitud);
    }
}
