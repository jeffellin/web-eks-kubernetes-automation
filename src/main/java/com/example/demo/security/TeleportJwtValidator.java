package com.example.demo.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class TeleportJwtValidator {

    private static final Logger logger = LoggerFactory.getLogger(TeleportJwtValidator.class);

    @Value("${teleport.jwt.issuer}")
    private String expectedIssuer;

    @Value("${teleport.jwt.jwks-url}")
    private String jwksUrl;

    @Value("${teleport.jwt.validate-audience:true}")
    private boolean validateAudience;

    private JWKSet jwkSet;
    private long jwkSetLastFetched = 0;
    private static final long JWKS_CACHE_TTL_MS = TimeUnit.HOURS.toMillis(1); // Cache for 1 hour

    private final ConcurrentHashMap<String, JWK> keyCache = new ConcurrentHashMap<>();

    /**
     * Validates a Teleport JWT token
     * @param token The JWT token string
     * @return The validated claims if successful
     * @throws Exception if validation fails
     */
    public JWTClaimsSet validateToken(String token) throws Exception {
        // Parse the JWT
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Get the key ID from the JWT header
        String keyId = signedJWT.getHeader().getKeyID();
        if (keyId == null) {
            throw new SecurityException("JWT does not contain a key ID (kid)");
        }

        // Get the public key from JWKS
        JWK jwk = getKey(keyId);
        if (jwk == null) {
            throw new SecurityException("Unable to find public key for key ID: " + keyId);
        }

        // Verify the signature
        JWSVerifier verifier = createVerifier(jwk);
        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("JWT signature verification failed");
        }

        // Validate claims
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        validateClaims(claims);

        logger.debug("JWT validated successfully for subject: {}", claims.getSubject());
        return claims;
    }

    private void validateClaims(JWTClaimsSet claims) throws Exception {
        // Validate issuer
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new SecurityException(
                String.format("Invalid issuer. Expected: %s, Got: %s",
                    expectedIssuer, claims.getIssuer())
            );
        }

        // Validate expiration
        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null) {
            throw new SecurityException("JWT does not contain expiration time");
        }
        if (expirationTime.before(Date.from(Instant.now()))) {
            throw new SecurityException("JWT has expired");
        }

        // Validate not before
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && notBefore.after(Date.from(Instant.now()))) {
            throw new SecurityException("JWT not yet valid (nbf claim)");
        }

        // Validate audience if configured
        if (validateAudience) {
            if (claims.getAudience() == null || claims.getAudience().isEmpty()) {
                logger.warn("JWT does not contain audience claim but validation is enabled");
            }
        }

        logger.debug("JWT claims validated successfully");
    }

    private JWSVerifier createVerifier(JWK jwk) throws Exception {
        if (jwk instanceof RSAKey) {
            return new RSASSAVerifier((RSAKey) jwk);
        } else if (jwk instanceof ECKey) {
            return new ECDSAVerifier((ECKey) jwk);
        } else {
            throw new SecurityException("Unsupported key type: " + jwk.getKeyType());
        }
    }

    private JWK getKey(String keyId) throws Exception {
        // Check cache first
        JWK cachedKey = keyCache.get(keyId);
        if (cachedKey != null) {
            return cachedKey;
        }

        // Fetch JWKS if needed
        refreshJwksIfNeeded();

        // Find the key by ID
        JWK key = jwkSet.getKeyByKeyId(keyId);
        if (key != null) {
            keyCache.put(keyId, key);
        }

        return key;
    }

    private synchronized void refreshJwksIfNeeded() throws Exception {
        long now = System.currentTimeMillis();

        // Refresh if cache expired or not yet loaded
        if (jwkSet == null || (now - jwkSetLastFetched) > JWKS_CACHE_TTL_MS) {
            logger.info("Fetching JWKS from: {}", jwksUrl);
            jwkSet = JWKSet.load(new URL(jwksUrl));
            jwkSetLastFetched = now;
            keyCache.clear(); // Clear key cache when JWKS is refreshed
            logger.info("JWKS fetched successfully, contains {} keys", jwkSet.getKeys().size());
        }
    }

    /**
     * Force refresh of the JWKS (useful for testing or key rotation)
     */
    public void forceRefreshJwks() throws Exception {
        jwkSetLastFetched = 0;
        refreshJwksIfNeeded();
    }
}
