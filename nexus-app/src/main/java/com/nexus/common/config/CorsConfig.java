package com.nexus.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration — allows the frontend (Next.js) to call the API
 * from a different origin (localhost:3000 in dev, production URL in prod).
 *
 * <p>This bean is picked up by Spring Security's {@code .cors()} integration
 * and also by Spring MVC's CORS handling.
 *
 * <p><b>Security note:</b> {@code allowCredentials(true)} is required because
 * the frontend sends the JWT in the Authorization header. When credentials
 * are enabled, {@code allowedOrigins("*")} is not allowed — we must
 * enumerate specific origins.
 */
@Configuration
public class CorsConfig {

    @Value("${nexus.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/oauth2/**", config);
        source.registerCorsConfiguration("/login/oauth2/**", config);
        return source;
    }
}
