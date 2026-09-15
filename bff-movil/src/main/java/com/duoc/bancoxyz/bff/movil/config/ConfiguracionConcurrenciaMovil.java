package com.duoc.bancoxyz.bff.movil.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ejecutor para las llamadas al core.
 *
 * <p>El resumen necesita la cuenta y sus ultimos movimientos, que son dos
 * recursos distintos del core. Pedirlos uno tras otro haria pagar dos latencias
 * donde basta una; con hilos virtuales, lanzarlos en paralelo no cuesta un hilo
 * de plataforma por llamada.</p>
 */
@Configuration
public class ConfiguracionConcurrenciaMovil {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService ejecutorLlamadasCoreMovil() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
