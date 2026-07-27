package com.cadence.companyservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI companyServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cadence Company Service API")
                        .description("Company profile, departments, offices and team invitations for the Cadence AI Hiring Platform")
                        .version("1.0.0")
                        .contact(new Contact().name("Cadence Platform Team")));
    }
}
