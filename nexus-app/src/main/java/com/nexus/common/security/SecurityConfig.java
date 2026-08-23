package com.nexus.common.security;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.nexus.common.security.oauth2.OAuth2LoginSuccessHandler;

/**
 * Central security configuration for the Nexus API.
 *
 * <p><b>Key decisions:</b></p>
 * <ul>
 *   <li><b>Stateless sessions</b> — no HTTP session, no cookies. Every request
 *       carries its own JWT token in the {@code Authorization: Bearer ...} header.</li>
 *   <li><b>CSRF disabled</b> — CSRF protection is for cookie-based sessions.
 *       Since we're stateless (no cookies), CSRF attacks don't apply.</li>
 *   <li><b>HMAC-SHA256 JWT</b> — symmetric signing with a shared secret.
 *       Simpler than RSA for a single-service deployment. If we ever split into
 *       microservices, we'd switch to RSA (public key verification without sharing
 *       the private key).</li>
 *   <li><b>Method security enabled</b> — {@code @PreAuthorize} annotations on
 *       controller/service methods enforce role-based access control.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtDecoder jwtDecoder;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, JwtDecoder jwtDecoder) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Stateless for API requests — no HTTP session, JWT in every request.
                // OAuth2 login flow needs IF_REQUIRED because Google's redirect
                // requires session state to validate the callback.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // CORS — allow frontend (Next.js) to call the API
                .cors(Customizer.withDefaults())

                // CSRF disabled — not needed for stateless REST APIs
                .csrf(csrf -> csrf.disable())

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — login, health check, OAuth2 flow
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Swagger UI / OpenAPI (Phase 2)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**").permitAll()

                        .requestMatchers("/error").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Exception handling — API requests (/api/**) get 401 Bearer challenge.
                // Browser-initiated OAuth flow gets the default redirect.
                // Without this, oauth2Login's entry point redirects ALL unauthenticated
                // requests to Google — including API calls that should get 401.
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new org.springframework.security.oauth2.server.resource
                                        .web.BearerTokenAuthenticationEntryPoint(),
                                new org.springframework.security.web.util.matcher
                                        .AntPathRequestMatcher("/api/**")))

                // OAuth2 login — "Sign in with Google" flow
                // On success, our handler links the Google user to a Nexus account
                // and issues a JWT (not a session cookie)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler))

                // JWT validation — Spring Security's built-in OAuth2 Resource Server
                // automatically extracts the Bearer token, validates it with our
                // decoder, and populates the SecurityContext
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))

                .build();
    }


    /**
     * Converts JWT claims into Spring Security authorities.
     *
     * <p>By default, Spring's OAuth2 Resource Server reads the {@code scope}
     * claim and prefixes each value with {@code SCOPE_}. But we store roles
     * in a custom {@code roles} claim with the {@code ROLE_} prefix already
     * included (e.g., {@code ROLE_ADMIN}). This converter tells Spring to
     * read {@code roles} instead of {@code scope}, with no extra prefix.</p>
     */
    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        var grantedAuthoritiesConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // roles already have ROLE_ prefix

        var converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }


    /**
     * BCrypt password encoder — industry standard for hashing passwords.
     * Cost factor of 10 (default) takes ~100ms per hash — slow enough to
     * prevent brute-force attacks, fast enough for interactive login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager — used by AuthController to authenticate
     * email/password login requests. Spring auto-configures this with
     * our UserDetailsService + PasswordEncoder.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
