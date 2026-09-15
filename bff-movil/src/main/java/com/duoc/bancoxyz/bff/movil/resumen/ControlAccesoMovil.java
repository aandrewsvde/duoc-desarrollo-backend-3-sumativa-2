package com.duoc.bancoxyz.bff.movil.resumen;

import com.duoc.bancoxyz.seguridad.OperacionNoPermitidaException;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Identidad del canal movil.
 *
 * <p>A diferencia del canal web, aqui no existe la nocion de consultar la cuenta
 * de otro: la cuenta se toma del token y nunca de la peticion. Por eso este
 * componente no recibe un numero de cuenta que validar, sino que lo provee.</p>
 */
@Component
public class ControlAccesoMovil {

    public PrincipalCanal actual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PrincipalCanal canal && canal.cuentaAutorizada() != null) {
            return canal;
        }
        throw new OperacionNoPermitidaException("La sesion no corresponde al canal movil");
    }

    public int cuentaDeLaSesion() {
        return actual().cuentaAutorizada();
    }
}
