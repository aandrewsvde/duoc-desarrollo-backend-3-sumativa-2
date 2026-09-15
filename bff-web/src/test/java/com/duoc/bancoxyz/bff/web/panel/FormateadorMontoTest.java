package com.duoc.bancoxyz.bff.web.panel;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FormateadorMontoTest {

    @ParameterizedTest
    @CsvSource({
            "8000,      $8.000",
            "12000.00,  $12.000",
            "1500.60,   $1.501",
            "0,         $0",
            "1234567,   $1.234.567"
    })
    @DisplayName("formatea en pesos con separador de miles y sin decimales")
    void formateaEnPesos(String monto, String esperado) {
        assertThat(FormateadorMonto.formatear(new BigDecimal(monto))).isEqualTo(esperado);
    }

    @Test
    @DisplayName("un monto nulo no rompe la respuesta")
    void montoNulo() {
        assertThat(FormateadorMonto.formatear(null)).isEqualTo("$0");
    }
}
