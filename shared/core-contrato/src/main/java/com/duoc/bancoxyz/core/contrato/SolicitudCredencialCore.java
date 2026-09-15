package com.duoc.bancoxyz.core.contrato;

/** Validacion de usuario y contrasena para un canal concreto. */
public record SolicitudCredencialCore(String usuario, String password, String canal) {
}
