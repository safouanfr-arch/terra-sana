package com.example.demo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.PreparationGroupFinalizationDto;
import com.example.demo.dto.PreparationEmailStatusDto;
import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.service.PreparationEmailService;
import com.example.demo.service.PreparationGroupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/preparation")
@RequiredArgsConstructor
public class PreparationController {

    private final PreparationGroupService preparationGroupService;
    private final PreparationEmailService preparationEmailService;

    @PostMapping("/groupes/{creneauId}/finaliser")
    public PreparationGroupFinalizationDto finalizeGroup(@PathVariable("creneauId") Long creneauId) {
        return preparationGroupService.finalizeGroup(creneauId);
    }

    @GetMapping(value = "/groupes/{creneauId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("creneauId") Long creneauId) {
        PreparationPdfDocument pdf = preparationGroupService.generatePdf(creneauId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(pdf.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf.content());
    }

    @GetMapping("/groupes/{creneauId}/email")
    public PreparationEmailStatusDto getEmailStatus(@PathVariable("creneauId") Long creneauId) {
        return preparationEmailService.getStatus(creneauId);
    }

    @PostMapping("/groupes/{creneauId}/email")
    public PreparationEmailStatusDto sendEmail(@PathVariable("creneauId") Long creneauId) {
        return preparationEmailService.sendManually(creneauId);
    }
}
