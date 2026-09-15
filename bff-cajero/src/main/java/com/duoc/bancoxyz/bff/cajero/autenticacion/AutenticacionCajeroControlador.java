package com.duoc.bancoxyz.bff.cajero.autenticacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticacion del canal cajero. */
@RestController
@RequestMapping("/api/cajero/auth")
@Tag(name = "Autenticacion cajero", description = "Apertura de sesion con tarjeta y PIN")
public class AutenticacionCajeroControlador {

    private final ServicioAutenticacionCajero servicio;

    public AutenticacionCajeroControlador(ServicioAutenticacionCajero servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/pin")
    @Operation(summary = "Valida tarjeta y PIN y abre una sesion corta del canal CAJERO")
    public RespuestaSesionCajero autenticar(@Valid @RequestBody SolicitudPin solicitud) {
        return servicio.autenticar(solicitud);
    }
}
