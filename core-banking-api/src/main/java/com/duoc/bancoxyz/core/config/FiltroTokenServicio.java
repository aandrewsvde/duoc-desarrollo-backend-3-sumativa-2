package com.duoc.bancoxyz.core.config;

import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import com.duoc.bancoxyz.seguridad.ServicioTokens;
import com.duoc.bancoxyz.seguridad.TokenInvalidoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica al BFF que llama al core.
 *
 * <p>Verifica la firma y la audiencia del token de servicio, y a continuacion
 * resuelve los permisos <b>desde la configuracion del core</b>, ignorando por
 * completo los que venga declarando el token. Esa es la diferencia con el
 * filtro que usan los BFF frente al usuario final: alli el emisor del token es
 * el propio servicio que lo valida, mientras que aqui el emisor es un tercero
 * que comparte la clave y podria, si se le confiara, ampliarse el alcance a si
 * mismo.</p>
 */
public class FiltroTokenServicio extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroTokenServicio.class);
    private static final String PREFIJO = "Bearer ";

    private final ServicioTokens servicioTokens;
    private final PropiedadesCoreApi propiedades;

    public FiltroTokenServicio(ServicioTokens servicioTokens, PropiedadesCoreApi propiedades) {
        this.servicioTokens = servicioTokens;
        this.propiedades = propiedades;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        String encabezado = peticion.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            try {
                PrincipalCanal emisor = servicioTokens.validar(encabezado.substring(PREFIJO.length()).trim());

                Optional<PropiedadesCoreApi.ClienteServicio> cliente =
                        propiedades.buscarCliente(emisor.sujeto());

                if (cliente.isEmpty()) {
                    log.warn("Token con firma valida pero de un cliente no registrado: {}", emisor.sujeto());
                    SecurityContextHolder.clearContext();
                } else {
                    List<SimpleGrantedAuthority> autoridades = cliente.get().getPermisos().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    PrincipalCanal principalEfectivo = new PrincipalCanal(
                            emisor.sujeto(), "SERVICIO", List.of("ROLE_SERVICIO"),
                            cliente.get().getPermisos(), null, emisor.sujeto());

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principalEfectivo, null, autoridades));
                }
            } catch (TokenInvalidoException e) {
                SecurityContextHolder.clearContext();
                log.debug("Token de servicio rechazado en {}: {}", peticion.getRequestURI(), e.getMessage());
            }
        }

        cadena.doFilter(peticion, respuesta);
    }
}
