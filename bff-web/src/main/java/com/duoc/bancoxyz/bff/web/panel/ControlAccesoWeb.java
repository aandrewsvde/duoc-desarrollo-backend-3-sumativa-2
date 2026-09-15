package com.duoc.bancoxyz.bff.web.panel;

import com.duoc.bancoxyz.seguridad.OperacionNoPermitidaException;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Autorizacion a nivel de recurso del canal web.
 *
 * <p>Que el token sea valido solo dice que la persona entro por la puerta
 * correcta; no dice que pueda mirar <i>esta</i> cuenta. Sin esta comprobacion,
 * cualquier titular autenticado podria leer el panel de otro cambiando un numero
 * en la URL, que es la falla de autorizacion mas comun en APIs REST.</p>
 */
@Component
public class ControlAccesoWeb {

    private static final Logger log = LoggerFactory.getLogger(ControlAccesoWeb.class);
    private static final String PERMISO_TODAS = "VER_CUALQUIER_CUENTA";

    public PrincipalCanal exigirAccesoA(int numeroCuenta) {
        PrincipalCanal principal = actual();

        if (principal.tienePermiso(PERMISO_TODAS)) {
            return principal;
        }
        if (!principal.puedeOperarSobre(numeroCuenta)) {
            log.warn("Acceso denegado: el sujeto '{}' intento ver la cuenta {} estando habilitado para {}",
                    principal.sujeto(), numeroCuenta, principal.cuentaAutorizada());
            throw new OperacionNoPermitidaException(
                    "Su sesion no esta habilitada para consultar la cuenta " + numeroCuenta);
        }
        return principal;
    }

    public PrincipalCanal actual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PrincipalCanal canal) {
            return canal;
        }
        throw new OperacionNoPermitidaException("La sesion no corresponde al canal web");
    }
}
