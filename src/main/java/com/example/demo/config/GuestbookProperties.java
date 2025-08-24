package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "guestbook")
public class GuestbookProperties {
    
    private String theme = "light";
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public boolean isDarkTheme() {
        return "dark".equalsIgnoreCase(theme);
    }
}