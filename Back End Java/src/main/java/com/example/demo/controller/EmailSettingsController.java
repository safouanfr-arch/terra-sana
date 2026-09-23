package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmailSettingsDto;
import com.example.demo.service.EmailSettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/email-settings")
@RequiredArgsConstructor
public class EmailSettingsController {

    private final EmailSettingsService emailSettingsService;

    @GetMapping
    public EmailSettingsDto getSettings() {
        return emailSettingsService.getSettings();
    }

    @PutMapping
    public EmailSettingsDto updateSettings(@RequestBody EmailSettingsDto dto) {
        return emailSettingsService.saveSettings(dto);
    }
}
