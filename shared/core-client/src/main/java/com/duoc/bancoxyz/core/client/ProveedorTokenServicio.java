package com.duoc.bancoxyz.core.client;

import com.duoc.bancoxyz.seguridad.ConstantesServicio;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import com.duoc.bancoxyz.seguridad.PropiedadesToken;
import com.duoc.bancoxyz.seguridad.ServicioTokens;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Emite y cachea el token con el que este BFF se identifica ante el core.
 *
 * <p>Es una credencial <b>de servicio</b>, distinta de la del usuario final: el
 * core nunca ve el token del cliente. Esa separacion es intencional y es una de
 * las ventajas del patron: el token de un canal no circula mas alla de su
 * propio BFF, de modo que comprometer el core no expone las sesiones de los
 * usuarios ni al reves.</p>
 *
 * <p>El token se renueva cuando le queda menos de un tercio de vida. Emitirlo
 * en cada llamada seria un costo de firma innecesario; esperar a que expire
 * provocaria un 401 esporadico justo en el limite.</p>
 */
public class ProveedorTokenServicio {

    private static final double UMBRAL_RENOVACION = 1.0 / 3.0;

    private final ServicioTokens servicioTokens;
    private final String clienteId;
    private final long vigenciaSegundos;
    private final AtomicReference<TokenVigente> cache = new AtomicReference<>();

    private record TokenVigente(String token, Instant renovarDespuesDe) {
    }

    public ProveedorTokenServicio(PropiedadesCore propiedades) {
        this.clienteId = propiedades.getClienteId();
        this.vigenciaSegundos = propiedades.getVigenciaToken().toSeconds();
        this.servicioTokens = new ServicioTokens(new PropiedadesToken(
                ConstantesServicio.EMISOR_SERVICIO,
                ConstantesServicio.AUDIENCIA_CORE,
                propiedades.getSecretoServicio(),
                propiedades.getVigenciaToken()));
    }

    public String token() {
        TokenVigente vigente = cache.get();
        if (vigente != null && Instant.now().isBefore(vigente.renovarDespuesDe())) {
            return vigente.token();
        }
        String nuevo = servicioTokens.emitir(new PrincipalCanal(
                clienteId, "SERVICIO", List.of("ROLE_SERVICIO"), List.of("LEER_CORE"), null, clienteId));
        long margen = Math.max(1, (long) (vigenciaSegundos * UMBRAL_RENOVACION));
        cache.set(new TokenVigente(nuevo, Instant.now().plusSeconds(vigenciaSegundos - margen)));
        return nuevo;
    }
}
