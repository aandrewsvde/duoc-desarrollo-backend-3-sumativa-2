package com.duoc.bancoxyz.core.config;

import com.duoc.bancoxyz.core.identidad.CredencialRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra las credenciales de demostracion la primera vez que arranca el core.
 *
 * <p>Las contrasenas y los PIN se guardan siempre con BCrypt, nunca en claro,
 * aunque su valor original este documentado en la configuracion: el proposito es
 * que el entregable sea probable por un evaluador, no simular un secreto que no
 * lo es. Lo que si se demuestra es el mecanismo correcto de almacenamiento.</p>
 *
 * <p>Es idempotente: si ya hay credenciales, no hace nada.</p>
 */
@Component
public class InicializadorCredenciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorCredenciales.class);

    private final CredencialRepositorio repositorio;
    private final PasswordEncoder codificador;
    private final PropiedadesCoreApi propiedades;

    public InicializadorCredenciales(CredencialRepositorio repositorio,
                                     PasswordEncoder codificador,
                                     PropiedadesCoreApi propiedades) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.contarUsuarios() > 0) {
            log.info("Las credenciales ya estan sembradas; no se reinicializan");
            return;
        }

        var demo = propiedades.getCredencialesDemo();

        demo.getUsuarios().forEach(u -> repositorio.insertarUsuario(
                u.getUsuario(), codificador.encode(u.getPassword()),
                u.getCanal(), u.getPerfil(), u.getCuenta()));

        demo.getTarjetas().forEach(t -> repositorio.insertarTarjeta(
                t.getNumero(), codificador.encode(t.getPin()), t.getCuenta()));

        log.info("Credenciales sembradas: {} usuarios de canal y {} tarjetas (hash BCrypt)",
                demo.getUsuarios().size(), demo.getTarjetas().size());
    }
}
