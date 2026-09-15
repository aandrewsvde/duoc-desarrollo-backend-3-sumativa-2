package com.duoc.bancoxyz.seguridad;

/** El token no es utilizable en este canal: firma, audiencia o vigencia. */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }

    public TokenInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
