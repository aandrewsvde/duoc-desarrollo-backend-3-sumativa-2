package com.duoc.bancoxyz.core.contrato;

/**
 * Resultado de validar una credencial.
 *
 * <p>El core confirma <b>quien es</b> el sujeto y sobre que cuenta esta
 * habilitado, pero no decide su rol ni la vigencia de su sesion: eso lo define
 * cada BFF, porque es justamente lo que cambia entre canales.</p>
 */
public record IdentidadCore(
        boolean valida,
        String sujeto,
        String nombre,
        Integer cuentaId,
        String perfil,
        String motivo) {
}
