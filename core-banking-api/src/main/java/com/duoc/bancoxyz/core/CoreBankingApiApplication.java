package com.duoc.bancoxyz.core;

import com.duoc.bancoxyz.core.config.PropiedadesCoreApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Core bancario del Banco XYZ.
 *
 * <p>Publica el modelo completo y canonico del banco. No conoce a los canales
 * ni adapta nada a ellos: esa responsabilidad es de cada BFF. Mantener el core
 * ignorante de los canales es lo que permite agregar un cuarto sin modificarlo.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(PropiedadesCoreApi.class)
public class CoreBankingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApiApplication.class, args);
    }
}
