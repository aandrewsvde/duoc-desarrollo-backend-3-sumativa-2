package com.duoc.bancoxyz.bff.web.autenticacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticacion del canal web. */
@RestController
@RequestMapping("/api/web/auth")
@Tag(name = "Autenticacion web", description = "Apertura de sesion para navegadores")
public class AutenticacionWebControlador {

    private final ServicioAutenticacionWeb servicio;

    public AutenticacionWebControlador(ServicioAutenticacionWeb servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    @Operation(summary = "Abre una sesion web y devuelve un token del canal WEB")
    public RespuestaLoginWeb login(@Valid @RequestBody SolicitudLoginWeb solicitud) {
        return servicio.autenticar(solicitud);
    }
}
