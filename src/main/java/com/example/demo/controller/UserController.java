package com.example.demo.controller;

import com.example.demo.security.TeleportAuthentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof TeleportAuthentication teleportAuth) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", teleportAuth.getName());
            userInfo.put("email", teleportAuth.getEmail());
            userInfo.put("roles", teleportAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            userInfo.put("authenticated", true);

            return ResponseEntity.ok(userInfo);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", false);
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal String username) {
        TeleportAuthentication auth = (TeleportAuthentication)
            SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("email", auth.getEmail());
            userInfo.put("authorities", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());

            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.status(401).build();
    }
}
