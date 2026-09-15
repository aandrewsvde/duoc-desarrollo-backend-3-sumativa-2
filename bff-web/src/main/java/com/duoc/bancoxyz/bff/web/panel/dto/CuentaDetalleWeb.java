package com.duoc.bancoxyz.bff.web.panel.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Cuenta vista por el navegador.
 *
 * <p>Ademas del valor numerico se entrega el importe ya formateado y la
 * descripcion legible del tipo de cuenta. Son datos derivados que el canal web
 * podria calcular, pero resolverlos aqui evita que cada pantalla reimplante el
 * mismo formateo y que dos vistas muestren el mismo saldo de forma distinta.</p>
 */
public record CuentaDetalleWeb(
        int numeroCuenta,
        String titular,
        String tipoCuenta,
        String tipoCuentaDescripcion,
        BigDecimal saldoActual,
        String saldoFormateado,
        String moneda,
        int edadTitular,
        String estado,
        OffsetDateTime consultadoEn) {
}
