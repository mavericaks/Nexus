package com.nexus.common.security;

import com.nexus.common.security.dto.LoginRequest;
import com.nexus.common.security.dto.LoginResponse;
import com.nexus.common.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Authentication controller — handles login and returns JWT tokens.
 *
 * <p>This endpoint is permitted without authentication (configured in
 * {@link SecurityConfig}).
 *
 * <p><b>Login flow:</b></p>
 * <ol>
 *   <li>Client sends {@code POST /api/v1/auth/login} with email + password</li>
 *   <li>{@link AuthenticationManager} delegates to our
 *       {@code NexusUserDetailsService} to load the user, then BCrypt-compares
 *       the password</li>
 *   <li>If valid, we generate a JWT with the user's identity + roles</li>
 *   <li>Client stores the JWT and sends it in {@code Authorization: Bearer ...}
 *       on subsequent requests</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticate with email and password, returning a JWT token.
     *
     * <p>If credentials are invalid, Spring Security's
     * {@link AuthenticationManager} throws {@code BadCredentialsException},
     * which our {@code GlobalExceptionHandler} catches and maps to 401.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Spring Security authenticates: loads user via UserDetailsService,
        // compares BCrypt hash, throws BadCredentialsException if wrong
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // Extract identity from the authenticated principal
        NexusUserDetails userDetails = (NexusUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = jwtTokenProvider.generateToken(
                userDetails.getUsername(),
                userDetails.getUserId(),
                userDetails.getTenantId(),
                roles
        );

        return ResponseEntity.ok(new LoginResponse(
                token,
                userDetails.getUsername(),
                userDetails.getTenantId().toString(),
                roles
        ));
    }
}
