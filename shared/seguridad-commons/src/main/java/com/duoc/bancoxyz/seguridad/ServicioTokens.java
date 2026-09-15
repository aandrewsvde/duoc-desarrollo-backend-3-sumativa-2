package com.duoc.bancoxyz.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;

/**
 * Emite y valida los tokens de un canal.
 *
 * <p>Se instancia una vez por canal con sus {@link PropiedadesToken}, de modo
 * que la clase no necesita saber que canales existen: es el BFF quien define su
 * emisor, su audiencia, su clave y su vigencia al construirla. Agregar un cuarto
 * canal no obliga a tocar este archivo.</p>
 */
public class ServicioTokens {

    private static final String CLAIM_CANAL = "canal";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISOS = "permisos";
    private static final String CLAIM_CUENTA = "cuenta";
    private static final String CLAIM_CONTEXTO = "contexto";

    private final PropiedadesToken propiedades;
    private final SecretKey clave;

    public ServicioTokens(PropiedadesToken propiedades) {
        this.propiedades = propiedades;
        this.clave = Keys.hmacShaKeyFor(propiedades.secreto().getBytes(StandardCharsets.UTF_8));
    }

    public String emitir(PrincipalCanal principal) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(propiedades.vigencia());

        return Jwts.builder()
                .issuer(propiedades.emisor())
                .audience().add(propiedades.audiencia()).and()
                .subject(principal.sujeto())
                .claim(CLAIM_CANAL, principal.canal())
                .claim(CLAIM_ROLES, principal.roles())
                .claim(CLAIM_PERMISOS, principal.permisos())
                .claim(CLAIM_CUENTA, principal.cuentaAutorizada())
                .claim(CLAIM_CONTEXTO, principal.contexto())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
    }

    /**
     * Valida firma, emisor, audiencia y vigencia.
     *
     * <p>La audiencia se comprueba de forma explicita y no solo a traves del
     * parser: es la regla que impide que un token legitimo de un canal sirva en
     * otro, y conviene que sea visible en el codigo y no un efecto lateral de la
     * configuracion de la libreria.</p>
     */
    public PrincipalCanal validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(propiedades.emisor())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Set<String> audiencias = claims.getAudience();
            if (audiencias == null || !audiencias.contains(propiedades.audiencia())) {
                throw new TokenInvalidoException(
                        "El token fue emitido para otra audiencia (" + audiencias
                                + "); este servicio solo acepta " + propiedades.audiencia());
            }

            return new PrincipalCanal(
                    claims.getSubject(),
                    claims.get(CLAIM_CANAL, String.class),
                    listaDe(claims, CLAIM_ROLES),
                    listaDe(claims, CLAIM_PERMISOS),
                    claims.get(CLAIM_CUENTA, Integer.class),
                    claims.get(CLAIM_CONTEXTO, String.class));

        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("Token no valido para este canal: " + e.getMessage(), e);
        }
    }

    public long vigenciaSegundos() {
        return propiedades.vigencia().toSeconds();
    }

    @SuppressWarnings("unchecked")
    private static List<String> listaDe(Claims claims, String nombre) {
        Object valor = claims.get(nombre);
        return valor instanceof List<?> lista ? (List<String>) lista : List.of();
    }
}
