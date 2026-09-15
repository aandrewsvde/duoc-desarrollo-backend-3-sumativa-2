package com.duoc.bancoxyz.core.identidad;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acceso a las credenciales de usuarios y tarjetas. */
@Repository
public class CredencialRepositorio {

    /** Fila de credencial; el hash nunca sale de esta capa hacia la API. */
    public record Credencial(String sujeto, String passwordHash, String perfil,
                             Integer cuentaId, boolean activo, String nombreTitular) {
    }

    private final JdbcTemplate jdbcTemplate;

    public CredencialRepositorio(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Credencial> buscarUsuario(String usuario, String canal) {
        return jdbcTemplate.query("""
                SELECT u.usuario, u.password_hash, u.perfil, u.cuenta_id, u.activo,
                       COALESCE(c.titular, '') AS titular
                FROM usuario_canal u
                LEFT JOIN cuenta c ON c.numero_cuenta = u.cuenta_id
                WHERE u.usuario = ? AND u.canal = ?
                """,
                (rs, fila) -> new Credencial(
                        rs.getString("usuario"), rs.getString("password_hash"),
                        rs.getString("perfil"),
                        rs.getObject("cuenta_id", Integer.class),
                        rs.getBoolean("activo"), rs.getString("titular")),
                usuario, canal).stream().findFirst();
    }

    public Optional<Credencial> buscarTarjeta(String numeroTarjeta) {
        return jdbcTemplate.query("""
                SELECT t.numero_tarjeta, t.pin_hash, t.cuenta_id, t.activa, c.titular
                FROM tarjeta t
                JOIN cuenta c ON c.numero_cuenta = t.cuenta_id
                WHERE t.numero_tarjeta = ?
                """,
                (rs, fila) -> new Credencial(
                        rs.getString("numero_tarjeta"), rs.getString("pin_hash"),
                        "TARJETAHABIENTE", rs.getInt("cuenta_id"),
                        rs.getBoolean("activa"), rs.getString("titular")),
                numeroTarjeta).stream().findFirst();
    }

    public long contarUsuarios() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuario_canal", Long.class);
        return total == null ? 0 : total;
    }

    public void insertarUsuario(String usuario, String hash, String canal, String perfil, Integer cuenta) {
        jdbcTemplate.update("""
                INSERT INTO usuario_canal (usuario, password_hash, canal, perfil, cuenta_id)
                VALUES (?, ?, ?, ?, ?)
                """, usuario, hash, canal, perfil, cuenta);
    }

    public void insertarTarjeta(String numero, String pinHash, Integer cuenta) {
        jdbcTemplate.update("""
                INSERT INTO tarjeta (numero_tarjeta, pin_hash, cuenta_id) VALUES (?, ?, ?)
                """, numero, pinHash, cuenta);
    }
}
