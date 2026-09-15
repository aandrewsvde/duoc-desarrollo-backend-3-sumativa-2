package com.duoc.bancoxyz.bff.movil.resumen.dto;

import java.util.List;

/**
 * Pantalla de inicio de la app.
 *
 * <p>Contiene exactamente lo que se ve al abrirla y nada mas: quien es, cuanto
 * tiene y sus ultimos movimientos. No viajan el tipo de cuenta, la edad del
 * titular, el estado, las descripciones de cada movimiento ni los agregados
 * anuales, todos ellos presentes en el modelo del core y en la respuesta del
 * canal web. Si el usuario entra al detalle, se piden entonces.</p>
 *
 * @param cuenta  numero de cuenta
 * @param titular nombre abreviado, suficiente para la cabecera
 * @param saldo   saldo en pesos, sin decimales
 * @param moneda  codigo ISO de la moneda
 * @param movs    ultimos movimientos
 */
public record ResumenMovil(
        int cuenta,
        String titular,
        long saldo,
        String moneda,
        List<MovimientoMovil> movs) {
}
