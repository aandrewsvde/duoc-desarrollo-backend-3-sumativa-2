package com.duoc.bancoxyz.core.retiro;

import com.duoc.bancoxyz.core.contrato.ResultadoRetiroCore;
import com.duoc.bancoxyz.core.contrato.SolicitudRetiroCore;
import com.duoc.bancoxyz.seguridad.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de retiros de efectivo.
 *
 * <p>Es la unica operacion del core que modifica dinero, y por eso concentra
 * tres cuidados que el resto de los endpoints no necesita:</p>
 *
 * <ol>
 *   <li><b>Bloqueo de la fila.</b> El saldo se lee con {@code FOR UPDATE}, de
 *       modo que dos retiros simultaneos sobre la misma cuenta se serializan.
 *       Sin el, ambos leerian el mismo saldo y podrian aprobarse los dos
 *       aunque juntos excedan el disponible.</li>
 *   <li><b>Idempotencia por referencia.</b> Si el cajero reenvia la orden tras
 *       un corte de red, la restriccion unica sobre la referencia rechaza la
 *       segunda insercion y se devuelve el resultado de la primera: el cliente
 *       recibe su comprobante y el dinero sale una sola vez.</li>
 *   <li><b>Atomicidad.</b> Descontar el saldo, registrar el movimiento y dejar
 *       el comprobante ocurren en la misma transaccion. Un fallo a mitad de
 *       camino no puede dejar dinero descontado sin movimiento que lo respalde.</li>
 * </ol>
 */
@Service
public class RetiroServicio {

    private static final Logger log = LoggerFactory.getLogger(RetiroServicio.class);

    private final JdbcTemplate jdbcTemplate;

    public RetiroServicio(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ResultadoRetiroCore registrar(int cuentaId, SolicitudRetiroCore solicitud) {
        Optional<ResultadoRetiroCore> yaProcesado = buscarPorReferencia(solicitud.referencia());
        if (yaProcesado.isPresent()) {
            log.info("Retiro con referencia repetida {}: se devuelve el resultado original",
                    solicitud.referencia());
            return yaProcesado.get();
        }

        BigDecimal saldoAnterior = bloquearSaldo(cuentaId);

        if (saldoAnterior.compareTo(solicitud.monto()) < 0) {
            log.info("Retiro rechazado por saldo insuficiente: cuenta={} solicitado={} disponible={}",
                    cuentaId, solicitud.monto(), saldoAnterior);
            return new ResultadoRetiroCore(false, cuentaId, solicitud.monto(), saldoAnterior,
                    saldoAnterior, null, OffsetDateTime.now(), "SALDO_INSUFICIENTE");
        }

        BigDecimal saldoResultante = saldoAnterior.subtract(solicitud.monto());
        String codigo = generarCodigoAutorizacion();

        jdbcTemplate.update("""
                UPDATE cuenta SET saldo = ?, actualizada_en = now() WHERE numero_cuenta = ?
                """, saldoResultante, cuentaId);

        jdbcTemplate.update("""
                INSERT INTO movimiento (cuenta_id, fecha, tipo, signo, monto, descripcion, canal_origen)
                VALUES (?, CURRENT_DATE, 'RETIRO', -1, ?, ?, 'CAJERO')
                """, cuentaId, solicitud.monto(),
                "Retiro por cajero " + solicitud.terminal());

        try {
            jdbcTemplate.update("""
                    INSERT INTO retiro (cuenta_id, monto, terminal, referencia, codigo_autorizacion,
                                        saldo_anterior, saldo_resultante)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, cuentaId, solicitud.monto(), solicitud.terminal(), solicitud.referencia(),
                    codigo, saldoAnterior, saldoResultante);
        } catch (DuplicateKeyException e) {
            // Otra peticion con la misma referencia gano la carrera: la transaccion
            // se revierte y se devuelve el resultado que quedo registrado.
            log.info("Carrera detectada en la referencia {}; se conserva el primer retiro",
                    solicitud.referencia());
            throw e;
        }

        log.info("Retiro aprobado: cuenta={} monto={} terminal={} autorizacion={} saldo {} -> {}",
                cuentaId, solicitud.monto(), solicitud.terminal(), codigo, saldoAnterior, saldoResultante);

        return new ResultadoRetiroCore(true, cuentaId, solicitud.monto(), saldoAnterior,
                saldoResultante, codigo, OffsetDateTime.now(), null);
    }

    private BigDecimal bloquearSaldo(int cuentaId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT saldo FROM cuenta WHERE numero_cuenta = ? FOR UPDATE",
                    BigDecimal.class, cuentaId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("La cuenta " + cuentaId + " no existe");
        }
    }

    private Optional<ResultadoRetiroCore> buscarPorReferencia(String referencia) {
        return jdbcTemplate.query("""
                SELECT cuenta_id, monto, codigo_autorizacion, saldo_anterior, saldo_resultante, instante
                FROM retiro WHERE referencia = ?
                """,
                (rs, fila) -> new ResultadoRetiroCore(
                        true,
                        rs.getInt("cuenta_id"),
                        rs.getBigDecimal("monto"),
                        rs.getBigDecimal("saldo_anterior"),
                        rs.getBigDecimal("saldo_resultante"),
                        rs.getString("codigo_autorizacion"),
                        rs.getTimestamp("instante").toInstant().atOffset(OffsetDateTime.now().getOffset()),
                        null),
                referencia).stream().findFirst();
    }

    private static String generarCodigoAutorizacion() {
        return "AUT-" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
    }
}
