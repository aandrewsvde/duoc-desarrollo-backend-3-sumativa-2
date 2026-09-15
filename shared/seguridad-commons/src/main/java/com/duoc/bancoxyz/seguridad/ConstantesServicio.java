package com.duoc.bancoxyz.seguridad;

/**
 * Identificadores del canal de servicio entre los BFF y el core.
 *
 * <p>Estan aqui, en un unico lugar compartido por emisor y verificador, porque
 * cuando el emisor y el receptor de un token declaran estas cadenas por separado
 * basta una discrepancia de una letra para que toda llamada devuelva 401 sin que
 * el mensaje de error diga por que.</p>
 */
public final class ConstantesServicio {

    /**
     * Emisor comun de los tokens de servicio.
     *
     * <p>Es el mismo para los tres BFF; al core no le sirve para distinguirlos.
     * Quien identifica al canal llamante es el <b>sujeto</b> del token, que el
     * core contrasta contra su lista de clientes registrados.</p>
     */
    public static final String EMISOR_SERVICIO = "banco-xyz-canales";

    /** Audiencia de los tokens dirigidos al core. */
    public static final String AUDIENCIA_CORE = "core-banking-api";

    private ConstantesServicio() {
    }
}
