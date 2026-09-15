package com.duoc.bancoxyz.core.cuenta;

import com.duoc.bancoxyz.core.contrato.ConteoTipoCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import com.duoc.bancoxyz.core.contrato.TotalMensualCore;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Agregados anuales de una cuenta.
 *
 * <p>Los tres calculos se resuelven en el motor de base de datos. Traer los 686
 * movimientos al proceso para sumarlos en Java costaria transferirlos y
 * recorrerlos en cada consulta de cada canal, cuando el resultado que se
 * necesita cabe en unas pocas filas.</p>
 */
@Repository
public class ResumenRepositorio {

    private final JdbcTemplate jdbcTemplate;

    public ResumenRepositorio(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumenAnualCore resumir(int cuentaId, int anio) {
        ResumenAnualCore base = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)                                                    AS total,
                       COALESCE(SUM(monto) FILTER (WHERE signo = 1), 0)            AS abonos,
                       COALESCE(SUM(monto) FILTER (WHERE signo = -1), 0)           AS cargos,
                       COALESCE(SUM(monto * signo), 0)                             AS neto,
                       MIN(fecha)                                                  AS primera,
                       MAX(fecha)                                                  AS ultima
                FROM movimiento
                WHERE cuenta_id = ? AND EXTRACT(YEAR FROM fecha) = ?
                """,
                (rs, fila) -> new ResumenAnualCore(
                        cuentaId, anio,
                        rs.getInt("total"),
                        rs.getBigDecimal("abonos"),
                        rs.getBigDecimal("cargos"),
                        rs.getBigDecimal("neto"),
                        fecha(rs.getDate("primera")),
                        fecha(rs.getDate("ultima")),
                        List.of(), List.of()),
                cuentaId, anio);

        List<ConteoTipoCore> porTipo = jdbcTemplate.query("""
                SELECT tipo, COUNT(*) AS cantidad, SUM(monto) AS monto
                FROM movimiento
                WHERE cuenta_id = ? AND EXTRACT(YEAR FROM fecha) = ?
                GROUP BY tipo
                ORDER BY SUM(monto) DESC
                """,
                (rs, fila) -> new ConteoTipoCore(
                        rs.getString("tipo"), rs.getInt("cantidad"), rs.getBigDecimal("monto")),
                cuentaId, anio);

        List<TotalMensualCore> porMes = jdbcTemplate.query("""
                SELECT to_char(fecha, 'YYYY-MM')                          AS mes,
                       COALESCE(SUM(monto) FILTER (WHERE signo = 1), 0)   AS abonos,
                       COALESCE(SUM(monto) FILTER (WHERE signo = -1), 0)  AS cargos
                FROM movimiento
                WHERE cuenta_id = ? AND EXTRACT(YEAR FROM fecha) = ?
                GROUP BY to_char(fecha, 'YYYY-MM')
                ORDER BY 1
                """,
                (rs, fila) -> new TotalMensualCore(
                        rs.getString("mes"), rs.getBigDecimal("abonos"), rs.getBigDecimal("cargos")),
                cuentaId, anio);

        return new ResumenAnualCore(
                base.cuentaId(), base.anio(), base.totalMovimientos(),
                base.totalAbonos(), base.totalCargos(), base.netoPeriodo(),
                base.primerMovimiento(), base.ultimoMovimiento(), porTipo, porMes);
    }

    private static LocalDate fecha(Date valor) {
        return valor == null ? null : valor.toLocalDate();
    }

    /** Saldo vigente, util para componer respuestas sin releer la cuenta completa. */
    public BigDecimal saldo(int cuentaId) {
        return jdbcTemplate.queryForObject(
                "SELECT saldo FROM cuenta WHERE numero_cuenta = ?", BigDecimal.class, cuentaId);
    }
}
