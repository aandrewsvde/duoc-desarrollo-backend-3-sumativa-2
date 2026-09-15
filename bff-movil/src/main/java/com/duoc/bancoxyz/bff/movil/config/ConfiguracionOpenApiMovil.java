package com.duoc.bancoxyz.bff.movil.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacion navegable del canal movil. */
@Configuration
public class ConfiguracionOpenApiMovil {

    @Bean
    public OpenAPI openApiMovil() {
        final String esquema = "tokenMovil";
        return new OpenAPI()
                .info(new Info()
                        .title("Banco XYZ - BFF Canal Movil")
                        .version("1.0.0")
                        .description("""
                                Backend dedicado a la aplicacion nativa. Entrega respuestas minimas:
                                campos abreviados, importes sin decimales y tipos en una letra.

                                Ningun endpoint recibe el numero de cuenta: se toma del token, de modo
                                que la app no puede consultar una cuenta ajena."""))
                .addSecurityItem(new SecurityRequirement().addList(esquema))
                .components(new Components().addSecuritySchemes(esquema,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/movil/auth/login")));
    }
}
