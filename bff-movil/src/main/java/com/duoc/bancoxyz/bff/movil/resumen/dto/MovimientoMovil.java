package com.duoc.bancoxyz.bff.movil.resumen.dto;

/**
 * Movimiento en su expresion minima.
 *
 * <p>Los nombres de campo son de una letra a proposito. En una lista de veinte
 * movimientos, las claves JSON se repiten veinte veces: llamarlos
 * {@code fecha}, {@code tipo} y {@code monto} costaria unos 200 bytes
 * adicionales por respuesta, que en un plan de datos movil y multiplicado por
 * cada apertura de la app deja de ser despreciable.</p>
 *
 * @param f fecha en ISO-8601
 * @param t tipo en una letra: D deposito, R retiro, C compra, P pago
 * @param m monto en pesos, sin decimales
 */
public record MovimientoMovil(String f, String t, long m) {
}
