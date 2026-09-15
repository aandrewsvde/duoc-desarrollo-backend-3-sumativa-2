package com.duoc.bancoxyz.core.cuenta;

import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Acceso paginado al historial de movimientos. */
@Repository
public class MovimientoRepositorio {

    private static final RowMapper<MovimientoCore> MAPEADOR = (rs, fila) -> new MovimientoCore(
            rs.getLong("id"),
            rs.getInt("cuenta_id"),
            rs.getDate("fecha").toLocalDate(),
            rs.getString("tipo"),
            rs.getInt("signo"),
            rs.getBigDecimal("monto"),
            rs.getString("descripcion"));

    private final JdbcTemplate jdbcTemplate;

    public MovimientoRepositorio(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Devuelve una pagina de movimientos, del mas reciente al mas antiguo.
     *
     * <p>La paginacion se resuelve en la base y no trayendo todo a memoria para
     * recortar despues: es la diferencia entre transferir una pagina o el
     * historial completo de la cuenta en cada consulta, y es la base sobre la
     * que despues cada canal decide cuantos movimientos pedir.</p>
     */
    public PaginaCore<MovimientoCore> listar(int cuentaId, int pagina, int tamano) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movimiento WHERE cuenta_id = ?", Long.class, cuentaId);
        long totalElementos = total == null ? 0L : total;

        List<MovimientoCore> contenido = jdbcTemplate.query("""
                SELECT id, cuenta_id, fecha, tipo, signo, monto, descripcion
                FROM movimiento
                WHERE cuenta_id = ?
                ORDER BY fecha DESC, id DESC
                LIMIT ? OFFSET ?
                """, MAPEADOR, cuentaId, tamano, (long) pagina * tamano);

        int totalPaginas = tamano == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamano);
        return new PaginaCore<>(contenido, pagina, tamano, totalElementos, totalPaginas);
    }
}
