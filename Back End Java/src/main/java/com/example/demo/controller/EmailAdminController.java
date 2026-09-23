package com.example.demo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmailTemplateDto;
import com.example.demo.dto.EmailTemplatePreviewRequestDto;
import com.example.demo.dto.GeneratedEmailDto;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.service.EmailTemplateService;
import com.example.demo.service.GeneratedEmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class EmailAdminController {

    private final EmailTemplateService emailTemplateService;
    private final GeneratedEmailService generatedEmailService;

    @GetMapping("/email-templates")
    public List<EmailTemplateDto> getTemplates() {
        emailTemplateService.ensureDefaultTemplates();
        return emailTemplateService.getAllTemplates();
    }

    @PutMapping("/email-templates/{type}")
    public EmailTemplateDto updateTemplate(
            @PathVariable("type") EmailNotificationType type,
            @RequestBody EmailTemplateDto dto
    ) {
        return emailTemplateService.updateTemplate(type, dto);
    }

    @PostMapping(value = "/email-templates/{type}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String previewTemplate(
            @PathVariable("type") EmailNotificationType type,
            @RequestBody(required = false) EmailTemplatePreviewRequestDto dto
    ) {
        return generatedEmailService.previewTemplate(type, dto);
    }

    @GetMapping("/email-notifications")
    public List<GeneratedEmailDto> getGeneratedEmails() {
        return generatedEmailService.getAll();
    }

    @GetMapping(value = "/email-notifications/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String previewGeneratedEmail(@PathVariable("id") Long id) {
        return generatedEmailService.previewGeneratedEmail(id);
    }

    @PostMapping("/email-notifications/send/order-reminder/{commandeId}")
    public GeneratedEmailDto sendOrderReminder(@PathVariable("commandeId") Long commandeId) {
        return generatedEmailService.sendOrderReminder(commandeId);
    }
}
