package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.order-preparation.email")
public class OrderPreparationEmailProperties {

    private boolean enabled = true;
    private String recipient = "";

    public boolean isConfigured() {
        return enabled && recipient != null && !recipient.isBlank();
    }
}
