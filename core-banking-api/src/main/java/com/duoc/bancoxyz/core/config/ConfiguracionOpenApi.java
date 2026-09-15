package com.duoc.bancoxyz.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacion navegable del contrato del core. */
@Configuration
public class ConfiguracionOpenApi {

    @Bean
    public OpenAPI openApiCore() {
        final String esquema = "tokenServicio";
        return new OpenAPI()
                .info(new Info()
                        .title("Banco XYZ - Core Bancario")
                        .version("1.0.0")
                        .description("""
                                Modelo canonico del banco: cuentas, movimientos, agregados anuales,
                                retiros e identidades. Es la unica fuente de verdad y no conoce a los
                                canales: son los BFF quienes adaptan estas respuestas a cada cliente.

                                Todo endpoint de negocio exige un token de servicio emitido por un BFF
                                registrado, y ademas el permiso correspondiente a la operacion."""))
                .addSecurityItem(new SecurityRequirement().addList(esquema))
                .components(new Components().addSecuritySchemes(esquema,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token de servicio del BFF llamante")));
    }
}
