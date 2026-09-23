package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.service.EmailSettingsService;
import com.example.demo.service.EmailTemplateService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailNotificationSeeder implements CommandLineRunner {

    private final EmailTemplateService emailTemplateService;
    private final EmailSettingsService emailSettingsService;

    @Override
    public void run(String... args) {
        emailTemplateService.ensureDefaultTemplates();
        emailSettingsService.initializeDefaults();
    }
}
