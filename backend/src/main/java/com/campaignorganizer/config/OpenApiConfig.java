package com.campaignorganizer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runtime Swagger UI metadata. The canonical contract remains
 * docs/api/openapi.yaml (see docs/adr/0008-contract-first-openapi.md); this
 * only mirrors it for the live /swagger-ui.html explorer.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI campaignOrganizerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Campaign Organizer API")
                        .version("0.1.0")
                        .description("Personal worldbuilding and RPG campaign manager.")
                        .license(new License().name("Proprietary (personal use)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
