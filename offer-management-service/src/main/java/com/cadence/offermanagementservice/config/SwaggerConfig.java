package com.cadence.offermanagementservice.config;

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
    public OpenAPI offerManagementServiceOpenAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Cadence Offer Management Service API")
                        .description("Offer lifecycle: draft, approval, PDF generation, send, candidate accept/decline/negotiate for the Cadence AI Hiring Platform")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme().name(schemeName).type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
