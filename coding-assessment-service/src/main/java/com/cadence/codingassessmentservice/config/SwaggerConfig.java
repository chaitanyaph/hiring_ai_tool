package com.cadence.codingassessmentservice.config;

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
    public OpenAPI codingAssessmentServiceOpenAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Cadence Coding Assessment Service API")
                        .description("Assessment/question management, online IDE, code execution (Judge0), AI code review (Gemini/Groq/Ollama) for the Cadence AI Hiring Platform")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme().name(schemeName).type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
