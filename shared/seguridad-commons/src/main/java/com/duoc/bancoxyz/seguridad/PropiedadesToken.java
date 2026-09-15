package com.duoc.bancoxyz.seguridad;

import java.time.Duration;

/**
 * Parametros del token de un canal.
 *
 * <p>Cada BFF define los suyos. Dos de estos campos son los que hacen que un
 * token no pueda cruzar de un canal a otro:</p>
 * <ul>
 *   <li>{@code secreto}: cada canal firma con una clave distinta, de modo que
 *       la firma de un token movil ni siquiera <i>verifica</i> contra la clave
 *       del cajero. Es una barrera criptografica, no una comprobacion que se
 *       pueda olvidar.</li>
 *   <li>{@code audiencia}: aunque dos canales llegaran a compartir clave, la
 *       audiencia declarada en el token debe coincidir con la del servicio que
 *       lo recibe. Es la segunda barrera, esta vez explicita y auditable.</li>
 * </ul>
 *
 * <p>La {@code vigencia} tambien es por canal y no por casualidad: un cajero
 * automatico opera en sesiones de segundos frente al usuario, mientras que una
 * sesion web acompana a la persona durante su jornada.</p>
 */
public record PropiedadesToken(
        String emisor,
        String audiencia,
        String secreto,
        Duration vigencia) {

    public PropiedadesToken {
        if (secreto == null || secreto.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "El secreto de firma debe tener al menos 32 bytes para HMAC-SHA256");
        }
    }
}
