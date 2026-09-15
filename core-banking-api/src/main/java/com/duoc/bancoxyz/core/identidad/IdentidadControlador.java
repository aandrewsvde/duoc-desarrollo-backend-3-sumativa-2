package com.duoc.bancoxyz.core.identidad;

import com.duoc.bancoxyz.core.contrato.IdentidadCore;
import com.duoc.bancoxyz.core.contrato.SolicitudCredencialCore;
import com.duoc.bancoxyz.core.contrato.SolicitudTarjetaCore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verificacion de credenciales para los canales.
 *
 * <p>Devuelve 200 tanto si la credencial es valida como si no, con el resultado
 * en el cuerpo. Responder 401 aqui seria confuso: el 401 corresponde al BFF
 * frente a su usuario, mientras que esta llamada la hace un servicio que si
 * esta correctamente autenticado y cuya pregunta se respondio sin problemas.</p>
 */
@RestController
@RequestMapping("/api/v1/identidades")
@Tag(name = "Identidades", description = "Verificacion de credenciales de usuario y de tarjeta")
public class IdentidadControlador {

    private final IdentidadServicio servicio;

    public IdentidadControlador(IdentidadServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/validar")
    @Operation(summary = "Valida usuario y contrasena en un canal")
    public IdentidadCore validar(@RequestBody SolicitudCredencialCore solicitud) {
        return servicio.validarUsuario(solicitud.usuario(), solicitud.password(), solicitud.canal());
    }

    @PostMapping("/validar-tarjeta")
    @Operation(summary = "Valida tarjeta y PIN; exclusivo del canal cajero")
    public IdentidadCore validarTarjeta(@RequestBody SolicitudTarjetaCore solicitud) {
        return servicio.validarTarjeta(solicitud.numeroTarjeta(), solicitud.pin());
    }
}
