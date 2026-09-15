package com.duoc.bancoxyz.bff.cajero.operacion.dto;

/**
 * Consulta de saldo.
 *
 * <p>La respuesta mas pequena de los tres canales: cuatro campos. No lleva
 * titular ni numero de cuenta completo, porque el cajero ya los tiene en
 * pantalla y porque se trata de un dispositivo publico.</p>
 *
 * @param disponible saldo en pesos, sin decimales
 */
public record SaldoCajero(
        long disponible,
        String moneda,
        String terminal,
        String instante) {
}
