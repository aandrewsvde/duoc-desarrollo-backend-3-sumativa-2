package com.duoc.bancoxyz.bff.web.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ejecutor para las llamadas al core del panel compuesto.
 *
 * <p>Se usan hilos virtuales: las tres llamadas son espera de red pura, y un
 * hilo de plataforma bloqueado en una lectura de socket es un recurso caro
 * desperdiciado. Con hilos virtuales, componer la respuesta en paralelo no
 * consume el pool de peticiones del servidor.</p>
 */
@Configuration
public class ConfiguracionConcurrenciaWeb {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService ejecutorLlamadasCore() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
