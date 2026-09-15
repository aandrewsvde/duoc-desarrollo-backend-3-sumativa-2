package com.duoc.bancoxyz.bff.web.autenticacion;

import com.duoc.bancoxyz.bff.web.config.PropiedadesCanalWeb;
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
 * Apertura de sesion en el canal web.
 *
 * <p>Aqui se ve con claridad la division de responsabilidades del patron: el
 * core confirma <i>quien es</i> la persona, y este BFF decide <i>que puede hacer
 * en la web</i> y <i>por cuanto tiempo</i>. Un ejecutivo y un titular se
 * autentican contra el mismo registro de usuarios, pero salen de aqui con roles
 * y alcances distintos.</p>
 */
@Service
public class ServicioAutenticacionWeb {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacionWeb.class);
    private static final String CANAL = "WEB";

    private static final List<String> PERMISOS_CLIENTE =
            List.of("VER_PANEL", "VER_MOVIMIENTOS", "VER_RESUMEN_ANUAL");
    private static final List<String> PERMISOS_EJECUTIVO =
            List.of("VER_PANEL", "VER_MOVIMIENTOS", "VER_RESUMEN_ANUAL", "VER_CUALQUIER_CUENTA");

    private final ClienteCoreBancario core;
    private final ServicioTokens tokens;
    private final PropiedadesCanalWeb propiedades;

    public ServicioAutenticacionWeb(ClienteCoreBancario core,
                                    ServicioTokens servicioTokensWeb,
                                    PropiedadesCanalWeb propiedades) {
        this.core = core;
        this.tokens = servicioTokensWeb;
        this.propiedades = propiedades;
    }

    public RespuestaLoginWeb autenticar(SolicitudLoginWeb solicitud) {
        IdentidadCore identidad = core.validarCredencial(
                solicitud.usuario(), solicitud.password(), CANAL);

        if (identidad == null || !identidad.valida()) {
            throw new BadCredentialsException("Usuario o contrasena incorrectos");
        }

        boolean esEjecutivo = "EJECUTIVO".equals(identidad.perfil());
        String rol = esEjecutivo ? "ROLE_EJECUTIVO_WEB" : "ROLE_CLIENTE_WEB";
        List<String> permisos = esEjecutivo ? PERMISOS_EJECUTIVO : PERMISOS_CLIENTE;

        // El ejecutivo no queda atado a una cuenta: su token no lleva cuenta
        // autorizada, y es el control de acceso del panel el que lo interpreta
        // como "cualquiera". Al titular, en cambio, el token lo ata a la suya.
        Integer cuentaAutorizada = esEjecutivo ? null : identidad.cuentaId();

        PrincipalCanal principal = new PrincipalCanal(
                identidad.sujeto(), CANAL, List.of(rol), permisos, cuentaAutorizada, "navegador");

        log.info("Sesion web abierta: usuario={} perfil={} cuenta={} vigencia={}h",
                identidad.sujeto(), identidad.perfil(), cuentaAutorizada,
                propiedades.getVigenciaToken().toHours());

        return new RespuestaLoginWeb(
                tokens.emitir(principal), "Bearer", tokens.vigenciaSegundos(),
                identidad.sujeto(), identidad.nombre(), identidad.perfil(),
                permisos, identidad.cuentaId());
    }
}
