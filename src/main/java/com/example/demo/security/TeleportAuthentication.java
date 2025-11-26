package com.example.demo.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TeleportAuthentication implements Authentication {

    private final String username;
    private final String email;
    private final JWTClaimsSet claims;
    private final List<GrantedAuthority> authorities;
    private boolean authenticated = true;

    public TeleportAuthentication(String username, String email, JWTClaimsSet claims) {
        this.username = username;
        this.email = email;
        this.claims = claims;
        this.authorities = extractAuthorities(claims);
    }

    private List<GrantedAuthority> extractAuthorities(JWTClaimsSet claims) {
        // Extract roles from JWT claims (adjust based on your Teleport config)
        try {
            List<String> roles = claims.getStringListClaim("roles");
            if (roles != null && !roles.isEmpty()) {
                return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return claims;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public JWTClaimsSet getClaims() {
        return claims;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return username;
    }
}
