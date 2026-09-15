package com.duoc.bancoxyz.bff.web.panel.dto;

import java.util.List;

/**
 * Pagina del historial.
 *
 * <p>Incluye {@code hayAnterior} y {@code haySiguiente} ya resueltos: son la
 * unica pregunta que la interfaz le hace a la paginacion, y calcularlos aqui
 * evita que cada cliente repita la misma aritmetica de indices.</p>
 */
public record PaginaMovimientosWeb(
        List<MovimientoWeb> movimientos,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean hayAnterior,
        boolean haySiguiente) {
}
