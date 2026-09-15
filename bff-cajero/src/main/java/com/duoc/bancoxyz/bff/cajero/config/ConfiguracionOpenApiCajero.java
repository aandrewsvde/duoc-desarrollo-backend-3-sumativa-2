package com.duoc.bancoxyz.bff.cajero.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacion navegable del canal cajero. */
@Configuration
public class ConfiguracionOpenApiCajero {

    @Bean
    public OpenAPI openApiCajero() {
        final String esquema = "tokenCajero";
        return new OpenAPI()
                .info(new Info()
                        .title("Banco XYZ - BFF Canal Cajero")
                        .version("1.0.0")
                        .description("""
                                Backend dedicado a los cajeros automaticos. Dos operaciones y nada mas:
                                consultar saldo y retirar efectivo.

                                Autenticacion con tarjeta y PIN, sesion de 120 segundos y permisos
                                separados por operacion. La cuenta sale siempre del token."""))
                .addSecurityItem(new SecurityRequirement().addList(esquema))
                .components(new Components().addSecuritySchemes(esquema,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/cajero/auth/pin")));
    }
}
