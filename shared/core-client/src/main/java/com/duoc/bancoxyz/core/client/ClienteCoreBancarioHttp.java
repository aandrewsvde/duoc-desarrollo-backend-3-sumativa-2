package com.duoc.bancoxyz.core.client;

import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.IdentidadCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import com.duoc.bancoxyz.core.contrato.ResultadoRetiroCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import com.duoc.bancoxyz.core.contrato.SolicitudCredencialCore;
import com.duoc.bancoxyz.core.contrato.SolicitudRetiroCore;
import com.duoc.bancoxyz.core.contrato.SolicitudTarjetaCore;
import com.duoc.bancoxyz.seguridad.RecursoNoEncontradoException;
import com.duoc.bancoxyz.seguridad.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/**
 * Implementacion HTTPS del contrato del core.
 *
 * <p>Toda llamada viaja sobre TLS y con el token de servicio de este BFF. El
 * token se obtiene del {@link ProveedorTokenServicio} en cada peticion porque
 * ahi esta la cache: pedirlo siempre mantiene el codigo simple y no cuesta,
 * mientras que guardarlo en un campo dejaria una credencial vencida tras el
 * primer vencimiento.</p>
 */
public class ClienteCoreBancarioHttp implements ClienteCoreBancario {

    private static final Logger log = LoggerFactory.getLogger(ClienteCoreBancarioHttp.class);

    private final RestClient restClient;
    private final ProveedorTokenServicio proveedorToken;

    public ClienteCoreBancarioHttp(RestClient restClient, ProveedorTokenServicio proveedorToken) {
        this.restClient = restClient;
        this.proveedorToken = proveedorToken;
    }

    @Override
    public CuentaCore obtenerCuenta(int numeroCuenta) {
        return get("/api/v1/cuentas/" + numeroCuenta, CuentaCore.class,
                "La cuenta " + numeroCuenta + " no existe");
    }

    @Override
    public PaginaCore<MovimientoCore> listarMovimientos(int numeroCuenta, int pagina, int tamano) {
        String ruta = "/api/v1/cuentas/%d/movimientos?pagina=%d&tamano=%d"
                .formatted(numeroCuenta, pagina, tamano);
        return restClient.get()
                .uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorToken.token())
                .retrieve()
                .onStatus(estado -> estado.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new RecursoNoEncontradoException("La cuenta " + numeroCuenta + " no existe");
                })
                .body(new ParameterizedTypeReference<PaginaCore<MovimientoCore>>() { });
    }

    @Override
    public ResumenAnualCore resumenAnual(int numeroCuenta, int anio) {
        return get("/api/v1/cuentas/%d/resumen-anual?anio=%d".formatted(numeroCuenta, anio),
                ResumenAnualCore.class, "La cuenta " + numeroCuenta + " no existe");
    }

    @Override
    public ResultadoRetiroCore registrarRetiro(int numeroCuenta, SolicitudRetiroCore solicitud) {
        log.debug("Enviando retiro al core: cuenta={} monto={} referencia={}",
                numeroCuenta, solicitud.monto(), solicitud.referencia());
        return restClient.post()
                .uri("/api/v1/cuentas/" + numeroCuenta + "/retiros")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorToken.token())
                .body(solicitud)
                .retrieve()
                .onStatus(estado -> estado.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new RecursoNoEncontradoException("La cuenta " + numeroCuenta + " no existe");
                })
                .onStatus(estado -> estado.value() == HttpStatus.BAD_REQUEST.value(), (req, res) -> {
                    throw new SolicitudInvalidaException("El core rechazo la orden de retiro");
                })
                .body(ResultadoRetiroCore.class);
    }

    @Override
    public IdentidadCore validarCredencial(String usuario, String password, String canal) {
        return restClient.post()
                .uri("/api/v1/identidades/validar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorToken.token())
                .body(new SolicitudCredencialCore(usuario, password, canal))
                .retrieve()
                .body(IdentidadCore.class);
    }

    @Override
    public IdentidadCore validarTarjeta(String numeroTarjeta, String pin) {
        return restClient.post()
                .uri("/api/v1/identidades/validar-tarjeta")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorToken.token())
                .body(new SolicitudTarjetaCore(numeroTarjeta, pin))
                .retrieve()
                .body(IdentidadCore.class);
    }

    private <T> T get(String ruta, Class<T> tipo, String mensajeNoEncontrado) {
        return restClient.get()
                .uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorToken.token())
                .retrieve()
                .onStatus(estado -> estado.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new RecursoNoEncontradoException(mensajeNoEncontrado);
                })
                .body(tipo);
    }
}
