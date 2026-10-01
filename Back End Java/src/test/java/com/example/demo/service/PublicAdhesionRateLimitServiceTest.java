package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class PublicAdhesionRateLimitServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-10T09:00:00Z");

    @Test
    void normalTrafficIsAccepted() {
        PublicAdhesionRateLimitService service = service(10, 3);

        assertThatCode(() -> {
            service.check("192.0.2.1", "one@terra.test");
            service.check("192.0.2.1", "two@terra.test");
        }).doesNotThrowAnyException();
    }

    @Test
    void repeatedEmailIsLimitedRegardlessOfRemoteAddress() {
        PublicAdhesionRateLimitService service = service(10, 2);

        service.check("192.0.2.1", " Person@Terra.Test ");
        service.check("192.0.2.2", "person@terra.test");

        assertRateLimited(() -> service.check("192.0.2.3", "PERSON@TERRA.TEST"));
    }

    @Test
    void repeatedIpIsLimitedAcrossDifferentEmails() {
        PublicAdhesionRateLimitService service = service(2, 10);

        service.check("192.0.2.10", "one@terra.test");
        service.check("192.0.2.10", "two@terra.test");

        assertRateLimited(() -> service.check("192.0.2.10", "three@terra.test"));
    }

    @Test
    void countersResetWithoutSleepingWhenWindowExpires() {
        PublicAdhesionRateLimitService service = service(1, 1);
        service.check("192.0.2.20", "person@terra.test");
        assertRateLimited(() -> service.check("192.0.2.20", "person@terra.test"));

        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(NOW.plusSeconds(600), ZoneOffset.UTC)
        );

        assertThatCode(() -> service.check("192.0.2.20", "person@terra.test"))
                .doesNotThrowAnyException();
    }

    private PublicAdhesionRateLimitService service(int maxPerIp, int maxPerEmail) {
        PublicAdhesionRateLimitService service = new PublicAdhesionRateLimitService(10, maxPerIp, maxPerEmail);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        return service;
    }

    private void assertRateLimited(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }
}
