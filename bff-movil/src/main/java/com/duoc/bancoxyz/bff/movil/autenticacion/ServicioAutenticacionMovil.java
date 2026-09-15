package com.duoc.bancoxyz.bff.movil.autenticacion;

import com.duoc.bancoxyz.bff.movil.config.PropiedadesCanalMovil;
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
 * Apertura de sesion movil.
 *
 * <p>Mismo registro de usuarios que la web, resultado distinto: aqui la sesion
 * dura minutos en vez de horas, el rol es uno solo y el token queda ligado al
 * dispositivo. Es la misma persona con una sesion gobernada por las reglas de
 * su canal.</p>
 */
@Service
public class ServicioAutenticacionMovil {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacionMovil.class);
    private static final String CANAL = "MOVIL";
    private static final List<String> PERMISOS = List.of("VER_RESUMEN", "VER_MOVIMIENTOS");

    private final ClienteCoreBancario core;
    private final ServicioTokens tokens;
    private final PropiedadesCanalMovil propiedades;

    public ServicioAutenticacionMovil(ClienteCoreBancario core,
                                      ServicioTokens servicioTokensMovil,
                                      PropiedadesCanalMovil propiedades) {
        this.core = core;
        this.tokens = servicioTokensMovil;
        this.propiedades = propiedades;
    }

    public RespuestaLoginMovil autenticar(SolicitudLoginMovil solicitud) {
        IdentidadCore identidad = core.validarCredencial(
                solicitud.usuario(), solicitud.password(), CANAL);

        if (identidad == null || !identidad.valida() || identidad.cuentaId() == null) {
            throw new BadCredentialsException("Usuario o contrasena incorrectos");
        }

        PrincipalCanal principal = new PrincipalCanal(
                identidad.sujeto(), CANAL, List.of("ROLE_CLIENTE_MOVIL"), PERMISOS,
                identidad.cuentaId(), solicitud.dispositivoId());

        log.info("Sesion movil abierta: usuario={} cuenta={} dispositivo={} vigencia={}min",
                identidad.sujeto(), identidad.cuentaId(), solicitud.dispositivoId(),
                propiedades.getVigenciaToken().toMinutes());

        return new RespuestaLoginMovil(
                tokens.emitir(principal), tokens.vigenciaSegundos(), identidad.cuentaId());
    }
}
