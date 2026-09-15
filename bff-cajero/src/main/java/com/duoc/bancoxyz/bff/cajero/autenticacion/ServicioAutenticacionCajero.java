package com.duoc.bancoxyz.bff.cajero.autenticacion;

import com.duoc.bancoxyz.bff.cajero.config.PropiedadesCanalCajero;
import com.duoc.bancoxyz.core.client.ClienteCoreBancario;
import com.duoc.bancoxyz.core.contrato.IdentidadCore;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import com.duoc.bancoxyz.seguridad.ServicioTokens;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * Apertura de sesion en un cajero.
 *
 * <p>Es el unico canal que no autentica con usuario y contrasena sino con
 * tarjeta y PIN, y el unico que llama al endpoint de validacion de tarjetas del
 * core: los otros dos BFF no tienen permiso para invocarlo. Esa es la
 * autenticacion especifica de canal llevada hasta el mecanismo mismo, no solo
 * hasta la duracion del token.</p>
 */
@Service
public class ServicioAutenticacionCajero {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacionCajero.class);
    private static final List<String> PERMISOS = List.of("CONSULTAR_SALDO", "RETIRAR_EFECTIVO");

    private final ClienteCoreBancario core;
    private final ServicioTokens tokens;
    private final PropiedadesCanalCajero propiedades;

    public ServicioAutenticacionCajero(ClienteCoreBancario core,
                                       ServicioTokens servicioTokensCajero,
                                       PropiedadesCanalCajero propiedades) {
        this.core = core;
        this.tokens = servicioTokensCajero;
        this.propiedades = propiedades;
    }

    public RespuestaSesionCajero autenticar(SolicitudPin solicitud) {
        IdentidadCore identidad = core.validarTarjeta(solicitud.numeroTarjeta(), solicitud.pin());

        if (identidad == null || !identidad.valida() || identidad.cuentaId() == null) {
            log.info("Autenticacion rechazada en el terminal {}", solicitud.terminal());
            throw new BadCredentialsException("Tarjeta o PIN incorrectos");
        }

        PrincipalCanal principal = new PrincipalCanal(
                identidad.sujeto(), "CAJERO", List.of("ROLE_CAJERO"), PERMISOS,
                identidad.cuentaId(), solicitud.terminal());

        log.info("Sesion de cajero abierta: tarjeta={} cuenta={} terminal={} vigencia={}s",
                identidad.sujeto(), identidad.cuentaId(), solicitud.terminal(),
                propiedades.getVigenciaToken().toSeconds());

        return new RespuestaSesionCajero(
                tokens.emitir(principal), tokens.vigenciaSegundos(),
                identidad.nombre(), solicitud.terminal());
    }
}
