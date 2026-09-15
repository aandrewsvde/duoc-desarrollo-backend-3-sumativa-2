package com.duoc.bancoxyz.bff.web.panel;

import java.util.Map;

/**
 * Traduccion de los codigos del core a texto para la interfaz.
 *
 * <p>Vive en el BFF y no en el core a proposito: el core publica codigos
 * estables y neutros, y cada canal decide como nombrarlos. El movil abrevia,
 * la web escribe la frase completa, y manana un canal en otro idioma cambiaria
 * solo su tabla sin tocar el modelo canonico.</p>
 */
public final class EtiquetasCatalogo {

    private static final Map<String, String> TIPOS_CUENTA = Map.of(
            "AHORRO", "Cuenta de ahorro",
            "PRESTAMO", "Credito de consumo",
            "HIPOTECA", "Credito hipotecario");

    private static final Map<String, String> TIPOS_MOVIMIENTO = Map.of(
            "DEPOSITO", "Deposito",
            "RETIRO", "Retiro de efectivo",
            "COMPRA", "Compra con tarjeta",
            "PAGO", "Pago de servicio");

    private static final String[] MESES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private EtiquetasCatalogo() {
    }

    public static String tipoCuenta(String codigo) {
        return TIPOS_CUENTA.getOrDefault(codigo, codigo);
    }

    public static String tipoMovimiento(String codigo) {
        return TIPOS_MOVIMIENTO.getOrDefault(codigo, codigo);
    }

    /** Convierte "2024-03" en "Marzo 2024". */
    public static String etiquetaMes(String mes) {
        if (mes == null || mes.length() != 7) {
            return mes;
        }
        int numero = Integer.parseInt(mes.substring(5));
        return MESES[numero - 1] + " " + mes.substring(0, 4);
    }
}
