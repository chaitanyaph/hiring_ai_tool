package com.cadence.resumeservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI resumeServiceOpenAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Cadence Resume Service API")
                        .description("Resume upload, storage, retrieval and access control for the Cadence AI Hiring Platform")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme().name(schemeName).type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
