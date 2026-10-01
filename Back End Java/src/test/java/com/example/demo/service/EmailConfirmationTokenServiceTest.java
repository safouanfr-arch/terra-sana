package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailConfirmationTokenServiceTest {

    private final EmailConfirmationTokenService service = new EmailConfirmationTokenService();

    @Test
    void generatedTokenUsesThirtyTwoRandomBytesAndStoresOnlySha256Hash() {
        var first = service.generate();
        var second = service.generate();

        assertThat(first.rawToken()).hasSize(43).matches("^[A-Za-z0-9_-]+$");
        assertThat(first.hash()).hasSize(64).matches("^[0-9a-f]+$");
        assertThat(first.hash()).isEqualTo(service.hash(first.rawToken()));
        assertThat(first.hash()).isNotEqualTo(first.rawToken());
        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
    }
}
