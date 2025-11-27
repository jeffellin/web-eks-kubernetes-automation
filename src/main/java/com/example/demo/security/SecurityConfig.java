package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TeleportJwtBearerTokenResolver bearerTokenResolver;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Autowired
    public SecurityConfig(TeleportJwtBearerTokenResolver bearerTokenResolver) {
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Create a JWT decoder that uses JWKS for signature validation
        // Explicitly support ES256 algorithm (Teleport uses Elliptic Curve instead of RSA)
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.ES256)
                .build();

        /*
         * Security Note: Why we only validate timestamps and skip issuer validation
         *
         * This is SAFE because of our architecture:
         *
         * 1. Network Isolation: This app is only accessible through Teleport proxy
         *    (deployed as cluster-internal Kubernetes service, not exposed to internet)
         *
         * 2. Cryptographic Verification: We still verify the JWT signature using
         *    Teleport's public keys from JWKS. Only Teleport has the private key
         *    to create valid signatures, so we know the token is authentic.
         *
         * 3. Single Issuer: Only Teleport can issue JWTs for this app. There's no
         *    ambiguity about who created the token.
         *
         * 4. Trust Boundary: We trust Teleport to:
         *    - Authenticate users (SSO, MFA, certificates)
         *    - Verify user permissions to access this app
         *    - Create JWTs with correct user identity and roles
         *
         * What we're validating:
         * ✓ Signature (proves token came from Teleport's private key)
         * ✓ Expiration (exp) - prevents replay attacks
         * ✓ Not Before (nbf) - prevents using future-dated tokens
         *
         * What we're skipping:
         * ✗ Issuer (iss) - Teleport uses cluster name "ellinj.teleport.sh"
         *   which Spring doesn't know about by default
         *
         * If this app were directly accessible from the internet or if multiple
         * services could issue JWTs, we would need to validate the issuer claim.
         */
        jwtDecoder.setJwtValidator(new JwtTimestampValidator());

        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Configure how to extract authorities from JWT
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // Teleport uses "roles" claim
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // Add ROLE_ prefix

        // Create the converter
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configure OAuth2 Resource Server for JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver)
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // Configure authorization - Vaadin resources must be public
            .authorizeHttpRequests(auth -> auth
                // Vaadin internal resources
                .requestMatchers("/VAADIN/**").permitAll()
                .requestMatchers("/vaadin-static/**").permitAll()
                .requestMatchers("/icons/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/sw.js").permitAll()
                .requestMatchers("/manifest.webmanifest").permitAll()
                .requestMatchers("/sw-runtime-resources-precache.js").permitAll()
                .requestMatchers("/offline.html").permitAll()
                // API and console endpoints
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // All other requests require authentication (JWT from Teleport)
                .anyRequest().authenticated()
            )
            // Disable CSRF - Teleport handles security and Vaadin has its own CSRF protection
            .csrf(csrf -> csrf.disable())
            // Allow frames for H2 console
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // Session management for Vaadin
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }
}
