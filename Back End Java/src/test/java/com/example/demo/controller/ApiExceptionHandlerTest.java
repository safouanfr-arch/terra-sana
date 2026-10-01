package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    @Test
    void dataAccessErrorDoesNotExposeTechnicalDetails() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "JDBC exception executing SQL [select secret from table]"
        );

        ResponseEntity<ProblemDetail> response = handler.handleDataAccessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail())
                .isEqualTo("Une erreur technique est survenue. Veuillez reessayer dans quelques instants.")
                .doesNotContain("JDBC", "SQL", "select", "secret");
    }
}
