package com.duoc.bancoxyz.seguridad;

/** El recurso solicitado no existe en el sistema. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
