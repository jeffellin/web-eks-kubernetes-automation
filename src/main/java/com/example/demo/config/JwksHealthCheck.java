package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class JwksHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(JwksHealthCheck.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @EventListener(ApplicationReadyEvent.class)
    public void checkJwksAccessibility() {
        try {
            logger.info("Testing JWKS endpoint accessibility: {}", jwkSetUri);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwkSetUri))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("✓ JWKS endpoint is accessible");
                logger.info("JWKS response: {}", response.body());
            } else {
                logger.error("✗ JWKS endpoint returned status: {}", response.statusCode());
                logger.error("Response: {}", response.body());
            }
        } catch (Exception e) {
            logger.error("✗ Failed to access JWKS endpoint: {}", e.getMessage(), e);
            logger.error("This will cause JWT validation to fail!");
        }
    }
}
