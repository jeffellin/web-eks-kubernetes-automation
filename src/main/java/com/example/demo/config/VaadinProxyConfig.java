package com.example.demo.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VaadinProxyConfig implements VaadinServiceInitListener {

    private static final Logger logger = LoggerFactory.getLogger(VaadinProxyConfig.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(initEvent -> {
            logger.info("Vaadin UI initialized successfully");
        });
    }
}