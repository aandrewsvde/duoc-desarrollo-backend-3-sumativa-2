package com.duoc.bancoxyz.seguridad;

/** La peticion incumple una regla de negocio del canal. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
