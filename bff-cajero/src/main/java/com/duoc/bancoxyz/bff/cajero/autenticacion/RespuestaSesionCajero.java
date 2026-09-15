package com.duoc.bancoxyz.bff.cajero.autenticacion;

/**
 * Sesion de cajero.
 *
 * <p>No devuelve el numero de cuenta ni el saldo: la pantalla del cajero saluda
 * al titular y espera que elija una operacion. Adelantar el saldo aqui lo
 * dejaria visible en una pantalla publica antes de que el usuario lo pida.</p>
 */
public record RespuestaSesionCajero(
        String token,
        long expiraEnSegundos,
        String titular,
        String terminal) {
}
