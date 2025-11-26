package com.example.demo.security;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TeleportJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TeleportJwtAuthenticationFilter.class);
    private static final String TELEPORT_JWT_HEADER = "Teleport-Jwt-Assertion";

    private final TeleportJwtValidator jwtValidator;

    public TeleportJwtAuthenticationFilter(TeleportJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwtToken = request.getHeader(TELEPORT_JWT_HEADER);

        if (jwtToken != null && !jwtToken.isEmpty()) {
            try {
                // Validate and verify the JWT signature
                JWTClaimsSet claims = jwtValidator.validateToken(jwtToken);

                // Extract user information from validated claims
                String username = claims.getSubject();
                String email = claims.getStringClaim("email");

                logger.info("Successfully validated Teleport JWT for user: {}", username);

                // Create authentication object with validated claims
                TeleportAuthentication authentication =
                    new TeleportAuthentication(username, email, claims);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (SecurityException e) {
                // Security validation failed - log and reject
                logger.error("JWT validation failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired JWT token\"}");
                return; // Don't continue filter chain
            } catch (Exception e) {
                // Unexpected error
                logger.error("Failed to validate Teleport JWT", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"JWT validation error\"}");
                return; // Don't continue filter chain
            }
        }

        filterChain.doFilter(request, response);
    }
}
