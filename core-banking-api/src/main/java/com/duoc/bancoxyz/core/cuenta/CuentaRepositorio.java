package com.duoc.bancoxyz.core.cuenta;

import com.duoc.bancoxyz.core.contrato.CuentaCore;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Acceso a cuentas. */
@Repository
public class CuentaRepositorio {

    private static final RowMapper<CuentaCore> MAPEADOR = (rs, fila) -> new CuentaCore(
            rs.getInt("numero_cuenta"),
            rs.getString("titular"),
            rs.getString("tipo_cuenta"),
            rs.getBigDecimal("saldo"),
            rs.getString("moneda"),
            rs.getInt("edad_titular"),
            rs.getString("estado"));

    private final JdbcTemplate jdbcTemplate;

    public CuentaRepositorio(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CuentaCore> buscar(int numeroCuenta) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT numero_cuenta, titular, tipo_cuenta, saldo, moneda, edad_titular, estado
                    FROM cuenta WHERE numero_cuenta = ?
                    """, MAPEADOR, numeroCuenta));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existe(int numeroCuenta) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cuenta WHERE numero_cuenta = ?", Integer.class, numeroCuenta);
        return total != null && total > 0;
    }
}
