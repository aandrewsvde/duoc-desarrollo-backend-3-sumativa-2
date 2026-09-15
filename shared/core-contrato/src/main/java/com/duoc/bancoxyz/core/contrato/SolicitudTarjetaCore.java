package com.duoc.bancoxyz.core.contrato;

/** Validacion de tarjeta y PIN, exclusiva del canal cajero. */
public record SolicitudTarjetaCore(String numeroTarjeta, String pin) {
}
