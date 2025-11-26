package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TeleportJwtValidator jwtValidator;

    @Autowired
    public SecurityConfig(TeleportJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Add Teleport JWT filter first with validation
        http.addFilterBefore(new TeleportJwtAuthenticationFilter(jwtValidator),
                UsernamePasswordAuthenticationFilter.class);

        // Configure authorization - Vaadin resources must be public
        http.authorizeHttpRequests(auth -> auth
                // Vaadin internal resources
                .requestMatchers(new AntPathRequestMatcher("/VAADIN/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/vaadin-static/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/icons/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/sw.js")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/manifest.webmanifest")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/sw-runtime-resources-precache.js")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/offline.html")).permitAll()
                // API and console endpoints
                .requestMatchers(new AntPathRequestMatcher("/api/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                // All other requests require authentication (JWT from Teleport)
                .anyRequest().authenticated()
        );

        // Disable CSRF - Teleport handles security and Vaadin has its own CSRF protection
        // Since we're behind Teleport proxy, this is safe
        http.csrf(csrf -> csrf.disable());

        // Allow frames for H2 console
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
        );

        // Session management for Vaadin
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        );

        return http.build();
    }
}
