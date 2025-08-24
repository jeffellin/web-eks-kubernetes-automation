package com.example.demo.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Theme(value = "lumo")
public class ThemeConfiguration implements AppShellConfigurator {
    
    private final GuestbookProperties guestbookProperties;
    
    @Autowired
    public ThemeConfiguration(GuestbookProperties guestbookProperties) {
        this.guestbookProperties = guestbookProperties;
    }
    
    @Override
    public void configurePage(com.vaadin.flow.server.AppShellSettings settings) {
        if (guestbookProperties.isDarkTheme()) {
            settings.addMetaTag("name", "theme-color");
            settings.addMetaTag("content", "#1a1a1a");
        }
    }
}