package com.duoc.bancoxyz.bff.web.autenticacion;

import java.util.List;

/**
 * Sesion abierta en el canal web.
 *
 * <p>Incluye el nombre del titular y las cuentas visibles porque la interfaz los
 * necesita de inmediato para pintar la cabecera; pedirlos en una segunda llamada
 * solo agregaria un viaje.</p>
 */
public record RespuestaLoginWeb(
        String token,
        String tipoToken,
        long expiraEnSegundos,
        String usuario,
        String titular,
        String perfil,
        List<String> permisos,
        Integer cuentaPrincipal) {
}
