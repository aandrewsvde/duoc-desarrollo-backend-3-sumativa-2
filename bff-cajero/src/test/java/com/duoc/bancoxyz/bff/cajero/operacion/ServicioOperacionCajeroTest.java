package com.duoc.bancoxyz.bff.cajero.operacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.duoc.bancoxyz.bff.cajero.config.PropiedadesCanalCajero;
import com.duoc.bancoxyz.core.client.ClienteCoreBancario;
import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.IdentidadCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import com.duoc.bancoxyz.core.contrato.ResultadoRetiroCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import com.duoc.bancoxyz.core.contrato.SolicitudRetiroCore;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import com.duoc.bancoxyz.seguridad.SolicitudInvalidaException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprueba las reglas de denominacion del cajero.
 *
 * <p>Se validan en el canal y no en el core porque son una limitacion del
 * dispositivo: un cajero cargado con billetes de mil no puede entregar 1.500
 * pesos, y el core no tiene por que saber que billetes lleva cada maquina.</p>
 */
class ServicioOperacionCajeroTest {

    /**
     * Doble del core que registra la ultima orden recibida. Se escribe a mano en
     * lugar de usar un marco de simulacion porque la interfaz es pequena y asi
     * la prueba no depende de nada mas que del contrato.
     */
    private static class CoreFalso implements ClienteCoreBancario {
        SolicitudRetiroCore ultimaOrden;

        @Override public CuentaCore obtenerCuenta(int numeroCuenta) {
            return new CuentaCore(numeroCuenta, "Diana Prince", "AHORRO",
                    new BigDecimal("8000.00"), "CLP", 35, "ACTIVA");
        }
        @Override public PaginaCore<MovimientoCore> listarMovimientos(int c, int p, int t) {
            return new PaginaCore<>(List.of(), p, t, 0, 0);
        }
        @Override public ResumenAnualCore resumenAnual(int c, int a) {
            throw new UnsupportedOperationException("el canal cajero no consulta resumenes");
        }
        @Override public ResultadoRetiroCore registrarRetiro(int cuenta, SolicitudRetiroCore orden) {
            this.ultimaOrden = orden;
            return new ResultadoRetiroCore(true, cuenta, orden.monto(),
                    new BigDecimal("8000.00"), new BigDecimal("8000.00").subtract(orden.monto()),
                    "AUT-123456", OffsetDateTime.now(), null);
        }
        @Override public IdentidadCore validarCredencial(String u, String p, String c) {
            throw new UnsupportedOperationException("el canal cajero no valida usuarios web");
        }
        @Override public IdentidadCore validarTarjeta(String n, String p) {
            return new IdentidadCore(true, n, "Diana Prince", 101, "TARJETAHABIENTE", null);
        }
    }

    private final CoreFalso core = new CoreFalso();
    private final PropiedadesCanalCajero propiedades = new PropiedadesCanalCajero();
    private final ServicioOperacionCajero servicio = new ServicioOperacionCajero(core, propiedades);

    private static PrincipalCanal sesion() {
        return new PrincipalCanal("**** 0101", "CAJERO", List.of("ROLE_CAJERO"),
                List.of("CONSULTAR_SALDO", "RETIRAR_EFECTIVO"), 101, "ATM-PRUEBA-01");
    }

    @Test
    @DisplayName("el saldo se entrega como entero y con el terminal de la sesion")
    void consultaDeSaldo() {
        var saldo = servicio.consultarSaldo(sesion());

        assertThat(saldo.disponible()).isEqualTo(8000L);
        assertThat(saldo.moneda()).isEqualTo("CLP");
        assertThat(saldo.terminal()).isEqualTo("ATM-PRUEBA-01");
    }

    @Test
    @DisplayName("un retiro valido devuelve comprobante con codigo de autorizacion")
    void retiroAprobado() {
        var comprobante = servicio.retirar(sesion(), new BigDecimal("20000"));

        assertThat(comprobante.aprobado()).isTrue();
        assertThat(comprobante.montoEntregado()).isEqualTo(20000L);
        assertThat(comprobante.saldoRestante()).isEqualTo(-12000L);
        assertThat(comprobante.codigoAutorizacion()).isEqualTo("AUT-123456");
        assertThat(comprobante.terminal()).isEqualTo("ATM-PRUEBA-01");
    }

    @Test
    @DisplayName("cada orden lleva una referencia unica que la hace idempotente en el core")
    void ordenLlevaReferenciaUnica() {
        servicio.retirar(sesion(), new BigDecimal("10000"));
        String primera = core.ultimaOrden.referencia();

        servicio.retirar(sesion(), new BigDecimal("10000"));
        String segunda = core.ultimaOrden.referencia();

        assertThat(primera).startsWith("ATM-ATM-PRUEBA-01-");
        assertThat(segunda).isNotEqualTo(primera);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1500", "999", "20500", "1"})
    @DisplayName("se rechaza todo monto que no sea multiplo de la denominacion")
    void rechazaMontosNoMultiplos(String monto) {
        assertThatThrownBy(() -> servicio.retirar(sesion(), new BigDecimal(monto)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("se rechaza por debajo del minimo por operacion")
    void rechazaBajoElMinimo() {
        assertThatThrownBy(() -> servicio.retirar(sesion(), new BigDecimal("0")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    @DisplayName("se rechaza sobre el maximo por operacion")
    void rechazaSobreElMaximo() {
        assertThatThrownBy(() -> servicio.retirar(sesion(), new BigDecimal("500000")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("maximo");
    }

    @Test
    @DisplayName("un rechazo del core se traduce a un mensaje legible en pantalla")
    void traduceElRechazoDelCore() {
        ClienteCoreBancario coreSinSaldo = new CoreFalso() {
            @Override public ResultadoRetiroCore registrarRetiro(int cuenta, SolicitudRetiroCore orden) {
                return new ResultadoRetiroCore(false, cuenta, orden.monto(),
                        new BigDecimal("1000"), new BigDecimal("1000"), null,
                        OffsetDateTime.now(), "SALDO_INSUFICIENTE");
            }
        };
        var servicioSinSaldo = new ServicioOperacionCajero(coreSinSaldo, propiedades);

        var comprobante = servicioSinSaldo.retirar(sesion(), new BigDecimal("50000"));

        assertThat(comprobante.aprobado()).isFalse();
        assertThat(comprobante.motivoRechazo()).isEqualTo("Saldo insuficiente para el monto solicitado");
        assertThat(comprobante.codigoAutorizacion()).isNull();
    }
}
