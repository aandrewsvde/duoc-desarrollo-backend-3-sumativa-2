package com.duoc.bancoxyz.bff.movil.resumen;

import com.duoc.bancoxyz.bff.movil.resumen.dto.MovimientoMovil;
import com.duoc.bancoxyz.bff.movil.resumen.dto.ResumenMovil;
import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Reduce el modelo canonico a lo que cabe en una pantalla de telefono.
 *
 * <p>Las tres reducciones que aplica, en orden de impacto:</p>
 * <ol>
 *   <li><b>Campos que no viajan.</b> De los siete atributos de una cuenta se
 *       envian tres; de los siete de un movimiento, tres. Es la mayor
 *       diferencia frente al canal web y no se recupera comprimiendo.</li>
 *   <li><b>Claves cortas.</b> En listas, el nombre del campo se repite una vez
 *       por elemento.</li>
 *   <li><b>Sin decimales.</b> El peso chileno no los usa, de modo que
 *       {@code 8000.00} se envia como {@code 8000}: tres caracteres menos por
 *       importe, y ademas un entero en vez de un decimal.</li>
 * </ol>
 */
@Component
public class EnsambladorMovil {

    /** Codigos de una letra; el canal web usa la palabra completa. */
    private static final Map<String, String> CODIGO_TIPO = Map.of(
            "DEPOSITO", "D",
            "RETIRO", "R",
            "COMPRA", "C",
            "PAGO", "P");

    public ResumenMovil ensamblar(CuentaCore cuenta, List<MovimientoCore> movimientos) {
        return new ResumenMovil(
                cuenta.numeroCuenta(),
                abreviarNombre(cuenta.titular()),
                aEntero(cuenta.saldo()),
                cuenta.moneda(),
                movimientos.stream().map(this::movimiento).toList());
    }

    public List<MovimientoMovil> movimientos(List<MovimientoCore> movimientos) {
        return movimientos.stream().map(this::movimiento).toList();
    }

    private MovimientoMovil movimiento(MovimientoCore origen) {
        return new MovimientoMovil(
                origen.fecha().toString(),
                CODIGO_TIPO.getOrDefault(origen.tipo(), "O"),
                aEntero(origen.monto()));
    }

    /** "Diana Prince" se convierte en "D. Prince": basta para la cabecera. */
    private static String abreviarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0];
        }
        return partes[0].charAt(0) + ". " + partes[partes.length - 1];
    }

    private static long aEntero(BigDecimal monto) {
        return monto == null ? 0L : monto.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
