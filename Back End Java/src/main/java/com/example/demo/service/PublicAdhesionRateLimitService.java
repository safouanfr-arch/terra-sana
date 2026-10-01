package com.example.demo.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicAdhesionRateLimitService {

    private static final String RATE_LIMIT_MESSAGE =
            "Trop de requetes. Veuillez reessayer dans quelques minutes.";

    private final Duration window;
    private final int maxPerIp;
    private final int maxPerEmail;
    private final ConcurrentMap<String, RequestBucket> buckets = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    public PublicAdhesionRateLimitService(
            @Value("${app.membership.rate-limit.window-minutes:10}") long windowMinutes,
            @Value("${app.membership.rate-limit.max-per-ip:10}") int maxPerIp,
            @Value("${app.membership.rate-limit.max-per-email:3}") int maxPerEmail
    ) {
        this.window = Duration.ofMinutes(windowMinutes);
        this.maxPerIp = maxPerIp;
        this.maxPerEmail = maxPerEmail;
    }

    public void check(String remoteAddress, String email) {
        Instant now = clock.instant();
        removeExpiredBuckets(now);
        consume("ip:" + normalizeRemoteAddress(remoteAddress), maxPerIp, now);
        consume("email:" + normalizeEmail(email), maxPerEmail, now);
    }

    private void consume(String key, int limit, Instant now) {
        AtomicBoolean rejected = new AtomicBoolean(false);
        buckets.compute(key, (ignored, current) -> {
            if (current == null || current.isExpiredAt(now, window)) {
                return new RequestBucket(now, 1);
            }
            if (current.count() >= limit) {
                rejected.set(true);
                return current;
            }
            return new RequestBucket(current.startedAt(), current.count() + 1);
        });

        if (rejected.get()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, RATE_LIMIT_MESSAGE);
        }
    }

    private void removeExpiredBuckets(Instant now) {
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpiredAt(now, window));
    }

    private String normalizeRemoteAddress(String remoteAddress) {
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record RequestBucket(Instant startedAt, int count) {
        private boolean isExpiredAt(Instant now, Duration window) {
            return !now.isBefore(startedAt.plus(window));
        }
    }
}
