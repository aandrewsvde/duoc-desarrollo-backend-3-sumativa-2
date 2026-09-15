package com.duoc.bancoxyz.bff.web.panel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EtiquetasCatalogoTest {

    @Test
    @DisplayName("traduce los codigos del core a texto para la interfaz")
    void traduceCodigos() {
        assertThat(EtiquetasCatalogo.tipoCuenta("AHORRO")).isEqualTo("Cuenta de ahorro");
        assertThat(EtiquetasCatalogo.tipoMovimiento("RETIRO")).isEqualTo("Retiro de efectivo");
    }

    @Test
    @DisplayName("un codigo desconocido se devuelve tal cual en vez de fallar")
    void codigoDesconocido() {
        assertThat(EtiquetasCatalogo.tipoCuenta("VISTA")).isEqualTo("VISTA");
        assertThat(EtiquetasCatalogo.tipoMovimiento("TRANSFERENCIA")).isEqualTo("TRANSFERENCIA");
    }

    @Test
    @DisplayName("convierte el mes ISO en una etiqueta legible")
    void etiquetaDelMes() {
        assertThat(EtiquetasCatalogo.etiquetaMes("2024-03")).isEqualTo("Marzo 2024");
        assertThat(EtiquetasCatalogo.etiquetaMes("2024-12")).isEqualTo("Diciembre 2024");
    }

    @Test
    @DisplayName("un mes mal formado se devuelve sin transformar")
    void mesInvalido() {
        assertThat(EtiquetasCatalogo.etiquetaMes("2024")).isEqualTo("2024");
        assertThat(EtiquetasCatalogo.etiquetaMes(null)).isNull();
    }
}
