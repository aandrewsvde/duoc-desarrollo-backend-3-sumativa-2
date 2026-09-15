package com.duoc.bancoxyz.bff.cajero.operacion;

import com.duoc.bancoxyz.bff.cajero.config.PropiedadesCanalCajero;
import com.duoc.bancoxyz.bff.cajero.operacion.dto.ComprobanteRetiro;
import com.duoc.bancoxyz.bff.cajero.operacion.dto.SaldoCajero;
import com.duoc.bancoxyz.core.client.ClienteCoreBancario;
import com.duoc.bancoxyz.core.contrato.ResultadoRetiroCore;
import com.duoc.bancoxyz.core.contrato.SolicitudRetiroCore;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import com.duoc.bancoxyz.seguridad.SolicitudInvalidaException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Operaciones del cajero.
 *
 * <p>Las reglas de denominacion se validan aqui, antes de molestar al core: un
 * cajero que solo carga billetes de mil no puede entregar 1.500 pesos, y eso es
 * una limitacion del dispositivo, no del banco. Rechazarlo en el canal evita un
 * viaje y mantiene al core libre de detalles de hardware.</p>
 */
@Service
public class ServicioOperacionCajero {

    private static final Logger log = LoggerFactory.getLogger(ServicioOperacionCajero.class);
    private static final DateTimeFormatter INSTANTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClienteCoreBancario core;
    private final PropiedadesCanalCajero propiedades;

    public ServicioOperacionCajero(ClienteCoreBancario core, PropiedadesCanalCajero propiedades) {
        this.core = core;
        this.propiedades = propiedades;
    }

    public SaldoCajero consultarSaldo(PrincipalCanal sesion) {
        var cuenta = core.obtenerCuenta(sesion.cuentaAutorizada());
        return new SaldoCajero(
                aEntero(cuenta.saldo()),
                cuenta.moneda(),
                sesion.contexto(),
                OffsetDateTime.now().format(INSTANTE));
    }

    public ComprobanteRetiro retirar(PrincipalCanal sesion, BigDecimal monto) {
        validarDenominacion(monto);

        // La referencia se genera aqui y acompana la orden: si la red se corta
        // despues de enviarla y el cajero reintenta, el core reconoce la misma
        // referencia y no entrega el dinero dos veces.
        String referencia = "ATM-" + sesion.contexto() + "-" + UUID.randomUUID();

        ResultadoRetiroCore resultado = core.registrarRetiro(
                sesion.cuentaAutorizada(),
                new SolicitudRetiroCore(monto, sesion.contexto(), referencia));

        if (!resultado.aprobado()) {
            log.info("Retiro rechazado en terminal {}: {}", sesion.contexto(), resultado.motivoRechazo());
            return new ComprobanteRetiro(false, 0, aEntero(resultado.saldoResultante()), "CLP",
                    null, sesion.contexto(), OffsetDateTime.now().format(INSTANTE),
                    traducirMotivo(resultado.motivoRechazo()));
        }

        log.info("Retiro entregado en terminal {}: monto={} autorizacion={}",
                sesion.contexto(), monto, resultado.codigoAutorizacion());

        return new ComprobanteRetiro(true,
                aEntero(resultado.montoRetirado()),
                aEntero(resultado.saldoResultante()),
                "CLP",
                resultado.codigoAutorizacion(),
                sesion.contexto(),
                OffsetDateTime.now().format(INSTANTE),
                null);
    }

    private void validarDenominacion(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException("El monto debe ser mayor que cero");
        }
        if (monto.compareTo(propiedades.getMontoMinimo()) < 0) {
            throw new SolicitudInvalidaException(
                    "El monto minimo por operacion es " + entero(propiedades.getMontoMinimo()));
        }
        if (monto.compareTo(propiedades.getMontoMaximo()) > 0) {
            throw new SolicitudInvalidaException(
                    "El monto maximo por operacion es " + entero(propiedades.getMontoMaximo()));
        }
        if (monto.remainder(propiedades.getMultiplo()).compareTo(BigDecimal.ZERO) != 0) {
            throw new SolicitudInvalidaException(
                    "El cajero solo entrega multiplos de " + entero(propiedades.getMultiplo()));
        }
    }

    /** El motivo del core se traduce a un mensaje que el cliente pueda leer en pantalla. */
    private static String traducirMotivo(String motivo) {
        return "SALDO_INSUFICIENTE".equals(motivo)
                ? "Saldo insuficiente para el monto solicitado"
                : "La operacion no pudo completarse";
    }

    private static long aEntero(BigDecimal valor) {
        return valor == null ? 0L : valor.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static String entero(BigDecimal valor) {
        return String.valueOf(aEntero(valor));
    }
}
