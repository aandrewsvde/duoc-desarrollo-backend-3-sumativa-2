package com.duoc.bancoxyz.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Traduce el encabezado {@code Authorization: Bearer ...} en una autenticacion
 * de Spring Security.
 *
 * <p>Los roles del token se publican como autoridades {@code ROLE_*} y los
 * permisos finos como autoridades sin prefijo, de modo que un endpoint puede
 * exigir {@code hasRole('CAJERO')} o {@code hasAuthority('RETIRAR_EFECTIVO')}
 * segun cuanta precision necesite.</p>
 *
 * <p>Si el token no es valido el filtro no lanza: deja el contexto vacio y sigue.
 * Quien decide como responder es la cadena de seguridad, que distingue un
 * recurso publico de uno protegido; abortar aqui romperia los endpoints
 * abiertos, como el de autenticacion.</p>
 */
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroAutenticacionJwt.class);
    private static final String ENCABEZADO = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final ServicioTokens servicioTokens;

    public FiltroAutenticacionJwt(ServicioTokens servicioTokens) {
        this.servicioTokens = servicioTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        String encabezado = peticion.getHeader(ENCABEZADO);
        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            String token = encabezado.substring(PREFIJO.length()).trim();
            try {
                PrincipalCanal principal = servicioTokens.validar(token);

                List<SimpleGrantedAuthority> autoridades = java.util.stream.Stream.concat(
                        principal.roles().stream(),
                        principal.permisos().stream()
                ).map(SimpleGrantedAuthority::new).toList();

                UsernamePasswordAuthenticationToken autenticacion =
                        new UsernamePasswordAuthenticationToken(principal, token, autoridades);
                SecurityContextHolder.getContext().setAuthentication(autenticacion);

            } catch (TokenInvalidoException e) {
                SecurityContextHolder.clearContext();
                log.debug("Token rechazado en {}: {}", peticion.getRequestURI(), e.getMessage());
            }
        }

        cadena.doFilter(peticion, respuesta);
    }
}
