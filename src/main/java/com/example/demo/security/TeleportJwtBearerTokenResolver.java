package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Custom resolver to extract JWT from Teleport's custom header
 * instead of the standard Authorization: Bearer header.
 */
@Component
public class TeleportJwtBearerTokenResolver implements BearerTokenResolver {

    private static final Logger logger = LoggerFactory.getLogger(TeleportJwtBearerTokenResolver.class);

    @Value("${teleport.jwt.header-name:Teleport-Jwt-Assertion}")
    private String headerName;

    @Override
    public String resolve(HttpServletRequest request) {
        // Extract JWT from custom Teleport header
        String token = request.getHeader(headerName);

        if (token != null) {
            logger.info("Found JWT in header '{}': {}", headerName, token.substring(0, Math.min(50, token.length())) + "...");
        } else {
            logger.warn("No JWT found in header '{}'", headerName);
        }

        return token;
    }
}
