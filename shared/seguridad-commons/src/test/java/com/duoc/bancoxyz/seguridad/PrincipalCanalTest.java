package com.duoc.bancoxyz.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrincipalCanalTest {

    private static PrincipalCanal conCuenta(Integer cuenta) {
        return new PrincipalCanal("sujeto", "WEB", List.of("ROLE_X"),
                List.of("VER_PANEL"), cuenta, "ctx");
    }

    @Test
    @DisplayName("un titular solo puede operar sobre la cuenta de su token")
    void titularAtadoASuCuenta() {
        PrincipalCanal titular = conCuenta(101);

        assertThat(titular.puedeOperarSobre(101)).isTrue();
        assertThat(titular.puedeOperarSobre(102)).isFalse();
    }

    @Test
    @DisplayName("un token sin cuenta declarada habilita cualquiera: es el caso del ejecutivo")
    void sinCuentaHabilitaTodas() {
        PrincipalCanal ejecutivo = conCuenta(null);

        assertThat(ejecutivo.puedeOperarSobre(101)).isTrue();
        assertThat(ejecutivo.puedeOperarSobre(999)).isTrue();
    }

    @Test
    void reconoceSusPermisos() {
        PrincipalCanal principal = conCuenta(101);

        assertThat(principal.tienePermiso("VER_PANEL")).isTrue();
        assertThat(principal.tienePermiso("RETIRAR_EFECTIVO")).isFalse();
    }
}
