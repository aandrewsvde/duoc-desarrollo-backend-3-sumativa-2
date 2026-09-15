package com.duoc.bancoxyz.core.contrato;

import java.util.List;

/** Pagina generica devuelta por el core. */
public record PaginaCore<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {
}
