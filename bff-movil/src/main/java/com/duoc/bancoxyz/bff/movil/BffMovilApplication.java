package com.duoc.bancoxyz.bff.movil;

import com.duoc.bancoxyz.bff.movil.config.PropiedadesCanalMovil;
import com.duoc.bancoxyz.core.client.ConfiguracionClienteCore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * BFF del canal movil.
 *
 * <p>Sirve a la aplicacion nativa, donde cada byte se paga en bateria, en plan
 * de datos y en segundos de espera bajo una senal debil. Su optimizacion es la
 * inversa a la del canal web: no compone vistas ricas sino que <b>recorta</b>.
 * Entrega lo minimo que la pantalla de inicio necesita y nada mas.</p>
 *
 * <p>Las tres decisiones que reducen el tamano son deliberadas y estan
 * documentadas en el ensamblador: nombres de campo cortos, importes sin
 * decimales -el peso chileno no los usa- y codigos de una letra para el tipo de
 * movimiento en lugar de la palabra completa.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(PropiedadesCanalMovil.class)
@Import(ConfiguracionClienteCore.class)
public class BffMovilApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffMovilApplication.class, args);
    }
}
