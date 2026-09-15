package com.duoc.bancoxyz.bff.movil.autenticacion;

/**
 * Sesion movil.
 *
 * <p>Deliberadamente mas escueta que la del canal web: no repite el perfil ni la
 * lista de permisos, porque la app tiene una sola pantalla y un solo rol. Lo que
 * no se usa, no viaja.</p>
 */
public record RespuestaLoginMovil(
        String token,
        long expiraEn,
        int cuenta) {
}
