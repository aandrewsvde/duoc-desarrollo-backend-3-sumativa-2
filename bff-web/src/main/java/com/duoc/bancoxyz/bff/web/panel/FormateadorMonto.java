package com.duoc.bancoxyz.bff.web.panel;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formateo de importes en pesos chilenos.
 *
 * <p>Los simbolos se fijan de forma explicita y no se toman del locale por
 * defecto de la maquina: de lo contrario, el mismo saldo se veria distinto en el
 * servidor de un desarrollador y en el de produccion.</p>
 *
 * <p>El peso chileno no usa decimales, de modo que se redondea a entero. Eso no
 * es solo una cuestion de presentacion: tambien recorta bytes en la respuesta.</p>
 */
public final class FormateadorMonto {

    private static final DecimalFormatSymbols SIMBOLOS = new DecimalFormatSymbols(Locale.ROOT);

    static {
        SIMBOLOS.setGroupingSeparator('.');
        SIMBOLOS.setDecimalSeparator(',');
    }

    private FormateadorMonto() {
    }

    public static String formatear(BigDecimal monto) {
        if (monto == null) {
            return "$0";
        }
        DecimalFormat formato = new DecimalFormat("$#,##0", SIMBOLOS);
        return formato.format(monto);
    }
}
