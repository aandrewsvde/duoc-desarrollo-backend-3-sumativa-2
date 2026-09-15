package com.duoc.bancoxyz.bff.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacion navegable del canal web. */
@Configuration
public class ConfiguracionOpenApiWeb {

    @Bean
    public OpenAPI openApiWeb() {
        final String esquema = "tokenWeb";
        return new OpenAPI()
                .info(new Info()
                        .title("Banco XYZ - BFF Canal Web")
                        .version("1.0.0")
                        .description("""
                                Backend dedicado al canal web. Entrega respuestas completas y
                                compuestas: el panel de una cuenta reune en una sola llamada lo que
                                el core publica en tres recursos, pidiendolos en paralelo.

                                El token de este canal no sirve en movil ni en cajero: cada canal
                                firma con su propia clave y declara su propia audiencia."""))
                .addSecurityItem(new SecurityRequirement().addList(esquema))
                .components(new Components().addSecuritySchemes(esquema,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/web/auth/login")));
    }
}
