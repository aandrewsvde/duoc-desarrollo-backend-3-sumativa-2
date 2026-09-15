package com.duoc.bancoxyz.seguridad;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/**
 * Cuerpo de error uniforme en los cuatro servicios.
 *
 * <p>Deliberadamente no expone rutas internas, nombres de tabla ni trazas: el
 * campo {@code detalle} lleva un mensaje pensado para quien consume la API, y
 * el diagnostico tecnico queda en el log del servidor asociado al mismo
 * {@code instante}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(
        OffsetDateTime instante,
        int estado,
        String error,
        String detalle,
        String ruta) {

    public static ErrorRespuesta de(int estado, String error, String detalle, String ruta) {
        return new ErrorRespuesta(OffsetDateTime.now(), estado, error, detalle, ruta);
    }
}
