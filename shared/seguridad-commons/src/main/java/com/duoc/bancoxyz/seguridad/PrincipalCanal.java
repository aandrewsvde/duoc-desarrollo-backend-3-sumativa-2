package com.duoc.bancoxyz.seguridad;

import java.util.List;

/**
 * Identidad autenticada dentro de un canal.
 *
 * @param sujeto           identificador del usuario, dispositivo o tarjeta
 * @param canal            canal que emitio el token (WEB, MOVIL, CAJERO)
 * @param roles            roles de Spring Security, ya con el prefijo ROLE_
 * @param permisos         permisos finos del canal (por ejemplo CONSULTAR_SALDO)
 * @param cuentaAutorizada cuenta sobre la que el sujeto puede operar; null
 *                         cuando el rol habilita a consultar varias, como el
 *                         ejecutivo del canal web
 * @param contexto         dato adicional propio del canal: el identificador del
 *                         dispositivo movil o el del terminal de cajero. Viaja
 *                         dentro del token para que no pueda alterarse en la
 *                         peticion, y queda disponible para trazabilidad.
 */
public record PrincipalCanal(
        String sujeto,
        String canal,
        List<String> roles,
        List<String> permisos,
        Integer cuentaAutorizada,
        String contexto) {

    /** Un rol amplio (ejecutivo) no queda atado a una cuenta concreta. */
    public boolean puedeOperarSobre(int numeroCuenta) {
        return cuentaAutorizada == null || cuentaAutorizada == numeroCuenta;
    }

    public boolean tienePermiso(String permiso) {
        return permisos != null && permisos.contains(permiso);
    }
}
