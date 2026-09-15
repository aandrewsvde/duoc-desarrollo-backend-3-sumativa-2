package com.duoc.bancoxyz.bff.web;

import com.duoc.bancoxyz.bff.web.config.PropiedadesCanalWeb;
import com.duoc.bancoxyz.core.client.ConfiguracionClienteCore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * BFF del canal web.
 *
 * <p>Sirve a navegadores de escritorio, donde el ancho de banda es holgado y la
 * pantalla admite mucha informacion. Su optimizacion no consiste en enviar menos
 * datos sino en enviar <b>menos veces</b>: compone en una sola respuesta lo que
 * el core publica en tres recursos, de modo que la pagina se pinta con una
 * llamada en lugar de encadenar tres y sufrir tres latencias.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(PropiedadesCanalWeb.class)
@Import(ConfiguracionClienteCore.class)
public class BffWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffWebApplication.class, args);
    }
}
