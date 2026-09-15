package com.duoc.bancoxyz.seguridad;

/**
 * El sujeto esta autenticado en el canal correcto pero intenta operar sobre un
 * recurso que su token no ampara; el caso tipico es consultar una cuenta ajena.
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
