package com.nexus.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for auto-generated Swagger UI.
 *
 * <p>Configures global JWT Bearer auth so that all endpoints show the
 * lock icon in Swagger UI. Users can paste their JWT token once and
 * test all endpoints interactively.
 *
 * <p>Available at:
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html}</li>
 *   <li>OpenAPI spec: {@code /v3/api-docs}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nexusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nexus API")
                        .description("""
                                AI-Powered Multi-Tenant Customer Support Platform.
                                
                                Nexus provides intelligent ticket triage, RAG-based knowledge retrieval,
                                and enterprise-grade multi-tenant isolation via PostgreSQL Row-Level Security.
                                
                                **Authentication:** All endpoints (except /api/v1/auth/**) require a JWT
                                Bearer token. Obtain one via POST /api/v1/auth/login or the Google OAuth flow.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Nexus Team")
                                .url("https://github.com/mavericaks/Nexus"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token")));
    }
}
