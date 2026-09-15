package com.duoc.bancoxyz.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifica las barreras que impiden que el token de un canal sirva en otro.
 * Es la garantia central del criterio de autenticacion y autorizacion por canal,
 * de modo que conviene tenerla cubierta y no solo configurada.
 */
class ServicioTokensTest {

    private static final String CLAVE_WEB = "clave-de-firma-del-canal-web-2026-32bytes";
    private static final String CLAVE_MOVIL = "clave-de-firma-del-canal-movil-2026-32by";

    private static ServicioTokens canal(String emisor, String audiencia, String clave, Duration vigencia) {
        return new ServicioTokens(new PropiedadesToken(emisor, audiencia, clave, vigencia));
    }

    private static PrincipalCanal principalDe(String canal, Integer cuenta) {
        return new PrincipalCanal("cliente101", canal, List.of("ROLE_" + canal),
                List.of("VER_SALDO"), cuenta, "contexto-" + canal);
    }

    private final ServicioTokens web =
            canal("bff-web", "canal-web", CLAVE_WEB, Duration.ofHours(8));
    private final ServicioTokens movil =
            canal("bff-movil", "canal-movil", CLAVE_MOVIL, Duration.ofMinutes(15));

    @Test
    @DisplayName("un token emitido por un canal se valida en ese canal y conserva sus datos")
    void emiteYValidaEnSuPropioCanal() {
        String token = web.emitir(principalDe("WEB", 101));

        PrincipalCanal validado = web.validar(token);

        assertThat(validado.sujeto()).isEqualTo("cliente101");
        assertThat(validado.canal()).isEqualTo("WEB");
        assertThat(validado.roles()).containsExactly("ROLE_WEB");
        assertThat(validado.permisos()).containsExactly("VER_SALDO");
        assertThat(validado.cuentaAutorizada()).isEqualTo(101);
        assertThat(validado.contexto()).isEqualTo("contexto-WEB");
    }

    @Nested
    @DisplayName("un token no cruza de un canal a otro")
    class AislamientoEntreCanales {

        @Test
        @DisplayName("porque la clave de firma es distinta en cada canal")
        void rechazaTokenDeOtroCanal() {
            String tokenMovil = movil.emitir(principalDe("MOVIL", 101));

            assertThatThrownBy(() -> web.validar(tokenMovil))
                    .isInstanceOf(TokenInvalidoException.class);
        }

        @Test
        @DisplayName("y aunque compartieran clave y emisor, la audiencia no coincide")
        void rechazaAudienciaAjenaConLaMismaClave() {
            // Mismo emisor y misma clave que el canal web: de ese modo la unica
            // diferencia es la audiencia, y la prueba aisla esa barrera en vez de
            // detenerse antes en la comprobacion del emisor.
            ServicioTokens mismaClaveOtraAudiencia =
                    canal("bff-web", "canal-movil", CLAVE_WEB, Duration.ofMinutes(15));
            String token = mismaClaveOtraAudiencia.emitir(principalDe("MOVIL", 101));

            assertThatThrownBy(() -> web.validar(token))
                    .isInstanceOf(TokenInvalidoException.class)
                    .hasMessageContaining("otra audiencia");
        }

        @Test
        @DisplayName("ni sirve un token de emisor desconocido")
        void rechazaEmisorDistinto() {
            ServicioTokens impostor =
                    canal("emisor-falso", "canal-web", CLAVE_WEB, Duration.ofHours(1));
            String token = impostor.emitir(principalDe("WEB", 101));

            assertThatThrownBy(() -> web.validar(token))
                    .isInstanceOf(TokenInvalidoException.class);
        }
    }

    @Test
    @DisplayName("un token vencido se rechaza")
    void rechazaTokenVencido() {
        ServicioTokens efimero =
                canal("bff-cajero", "canal-cajero", CLAVE_WEB, Duration.ofSeconds(-1));
        String token = efimero.emitir(principalDe("CAJERO", 101));

        assertThatThrownBy(() -> efimero.validar(token))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    @DisplayName("un token manipulado se rechaza")
    void rechazaTokenAlterado() {
        String token = web.emitir(principalDe("WEB", 101));
        String alterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> web.validar(alterado))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    @DisplayName("la vigencia configurada es la que se publica al cliente")
    void reportaVigencia() {
        assertThat(web.vigenciaSegundos()).isEqualTo(Duration.ofHours(8).toSeconds());
        assertThat(movil.vigenciaSegundos()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    }

    @Test
    @DisplayName("se rechaza una clave demasiado corta para HMAC-SHA256")
    void exigeClaveSuficiente() {
        assertThatThrownBy(() -> new PropiedadesToken("e", "a", "corta", Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }
}
