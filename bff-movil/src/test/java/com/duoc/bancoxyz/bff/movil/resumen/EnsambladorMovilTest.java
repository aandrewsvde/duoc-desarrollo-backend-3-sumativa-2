package com.duoc.bancoxyz.bff.movil.resumen;

import static org.assertj.core.api.Assertions.assertThat;

import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprueba las reducciones que definen al canal movil. Cada una es una decision
 * de diseno con un costo en bytes asociado, de modo que conviene fijarla con una
 * prueba y no dejarla a merced de un refactor distraido.
 */
class EnsambladorMovilTest {

    private final EnsambladorMovil ensamblador = new EnsambladorMovil();

    private static CuentaCore cuenta(String titular, String saldo) {
        return new CuentaCore(101, titular, "AHORRO", new BigDecimal(saldo), "CLP", 35, "ACTIVA");
    }

    private static MovimientoCore movimiento(String tipo, String monto) {
        return new MovimientoCore(7, 101, LocalDate.of(2024, 12, 22), tipo, -1,
                new BigDecimal(monto), "Una descripcion larga que no debe viajar al movil");
    }

    @Test
    @DisplayName("el nombre del titular se abrevia a inicial y apellido")
    void abreviaElNombre() {
        assertThat(ensamblador.ensamblar(cuenta("Diana Prince", "8000"), List.of()).titular())
                .isEqualTo("D. Prince");
    }

    @Test
    @DisplayName("un nombre de tres partes conserva la inicial y el ultimo apellido")
    void abreviaNombreCompuesto() {
        assertThat(ensamblador.ensamblar(cuenta("Maria Jose Soto", "8000"), List.of()).titular())
                .isEqualTo("M. Soto");
    }

    @Test
    @DisplayName("un nombre de una sola palabra se deja tal cual")
    void nombreSimpleSeConserva() {
        assertThat(ensamblador.ensamblar(cuenta("Prince", "8000"), List.of()).titular())
                .isEqualTo("Prince");
    }

    @Test
    @DisplayName("los importes viajan como enteros: el peso chileno no usa decimales")
    void importesSinDecimales() {
        var resumen = ensamblador.ensamblar(cuenta("Diana Prince", "8000.00"),
                List.of(movimiento("COMPRA", "1500.00")));

        assertThat(resumen.saldo()).isEqualTo(8000L);
        assertThat(resumen.movs().get(0).m()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("el tipo de movimiento se codifica en una letra")
    void tipoEnUnaLetra() {
        var movimientos = ensamblador.movimientos(List.of(
                movimiento("DEPOSITO", "1000"),
                movimiento("RETIRO", "1000"),
                movimiento("COMPRA", "1000"),
                movimiento("PAGO", "1000")));

        assertThat(movimientos).extracting(m -> m.t()).containsExactly("D", "R", "C", "P");
    }

    @Test
    @DisplayName("un tipo desconocido no rompe la respuesta")
    void tipoDesconocidoNoRompe() {
        assertThat(ensamblador.movimientos(List.of(movimiento("TRANSFERENCIA", "1000"))))
                .singleElement()
                .satisfies(m -> assertThat(m.t()).isEqualTo("O"));
    }

    @Test
    @DisplayName("la descripcion del movimiento no viaja al canal movil")
    void noViajaLaDescripcion() {
        var movimiento = ensamblador.movimientos(List.of(movimiento("COMPRA", "1500"))).get(0);

        // El record del canal movil solo tiene f, t y m: la descripcion no tiene
        // donde alojarse, y esa es justamente la reduccion que se busca.
        assertThat(movimiento.f()).isEqualTo("2024-12-22");
        assertThat(movimiento.t()).isEqualTo("C");
        assertThat(movimiento.m()).isEqualTo(1500L);
    }
}
