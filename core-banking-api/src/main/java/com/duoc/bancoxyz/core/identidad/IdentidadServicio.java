package com.duoc.bancoxyz.core.identidad;

import com.duoc.bancoxyz.core.contrato.IdentidadCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Comprobacion de credenciales.
 *
 * <p>El core responde una sola pregunta: si la credencial corresponde a alguien
 * y sobre que cuenta esta habilitado. No decide roles, ni permisos, ni cuanto
 * dura la sesion, porque eso cambia entre canales y es competencia de cada BFF.
 * Esa division es la que permite que el mismo titular tenga una sesion de ocho
 * horas en la web y de dos minutos en un cajero sin duplicar el registro de
 * usuarios.</p>
 *
 * <p>Ante una credencial incorrecta se devuelve siempre el mismo motivo
 * generico, sin distinguir si fallo el usuario o la contrasena: informar cual
 * de los dos existe le confirmaria a un atacante la mitad del par.</p>
 */
@Service
public class IdentidadServicio {

    private static final Logger log = LoggerFactory.getLogger(IdentidadServicio.class);
    private static final String MOTIVO_GENERICO = "CREDENCIAL_INVALIDA";

    private final CredencialRepositorio repositorio;
    private final PasswordEncoder codificador;

    public IdentidadServicio(CredencialRepositorio repositorio, PasswordEncoder codificador) {
        this.repositorio = repositorio;
        this.codificador = codificador;
    }

    public IdentidadCore validarUsuario(String usuario, String password, String canal) {
        var credencial = repositorio.buscarUsuario(usuario, canal);

        if (credencial.isEmpty() || !credencial.get().activo()
                || !codificador.matches(password, credencial.get().passwordHash())) {
            log.info("Autenticacion fallida en canal {} para el usuario '{}'", canal, enmascarar(usuario));
            return new IdentidadCore(false, null, null, null, null, MOTIVO_GENERICO);
        }

        var valida = credencial.get();
        log.info("Autenticacion correcta en canal {}: sujeto='{}' perfil={} cuenta={}",
                canal, enmascarar(usuario), valida.perfil(), valida.cuentaId());
        return new IdentidadCore(true, valida.sujeto(), valida.nombreTitular(),
                valida.cuentaId(), valida.perfil(), null);
    }

    public IdentidadCore validarTarjeta(String numeroTarjeta, String pin) {
        var credencial = repositorio.buscarTarjeta(numeroTarjeta);

        if (credencial.isEmpty() || !credencial.get().activo()
                || !codificador.matches(pin, credencial.get().passwordHash())) {
            log.info("Autenticacion de tarjeta fallida: {}", enmascararTarjeta(numeroTarjeta));
            return new IdentidadCore(false, null, null, null, null, MOTIVO_GENERICO);
        }

        var valida = credencial.get();
        log.info("Tarjeta autenticada: {} cuenta={}", enmascararTarjeta(numeroTarjeta), valida.cuentaId());
        return new IdentidadCore(true, enmascararTarjeta(numeroTarjeta), valida.nombreTitular(),
                valida.cuentaId(), valida.perfil(), null);
    }

    /** En el log nunca queda el identificador completo. */
    private static String enmascarar(String valor) {
        if (valor == null || valor.length() <= 3) {
            return "***";
        }
        return valor.substring(0, 3) + "***";
    }

    private static String enmascararTarjeta(String numero) {
        if (numero == null || numero.length() < 4) {
            return "****";
        }
        return "**** **** **** " + numero.substring(numero.length() - 4);
    }
}
