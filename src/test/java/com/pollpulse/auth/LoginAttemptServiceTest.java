package com.pollpulse.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private static final String EMAIL = "VOTER@POLLpulse.test";

    @Test
    void shouldLockAfterFiveFailures_whenFailuresAreConsecutive() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock, 5, 15);

        for (int attempt = 0; attempt < 4; attempt++) {
            service.recordFailure(EMAIL);
            assertFalse(service.isLocked(EMAIL));
        }
        service.recordFailure("  voter@pollpulse.test  ");

        assertTrue(service.isLocked(EMAIL));
    }

    @Test
    void shouldUnlockAfterFifteenMinutesAndResetCounter_whenLockExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock, 5, 15);

        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordFailure(EMAIL);
        }
        clock.advance(Duration.ofMinutes(14));
        service.recordFailure(EMAIL);
        clock.advance(Duration.ofMinutes(1));

        assertFalse(service.isLocked(EMAIL));
        for (int attempt = 0; attempt < 4; attempt++) {
            service.recordFailure(EMAIL);
        }
        assertFalse(service.isLocked(EMAIL));
        service.recordFailure(EMAIL);

        assertTrue(service.isLocked(EMAIL));
    }

    @Test
    void shouldResetFailureCounter_whenLoginSucceeds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock, 5, 15);

        for (int attempt = 0; attempt < 4; attempt++) {
            service.recordFailure(EMAIL);
        }
        service.recordSuccess(" voter@pollpulse.test ");
        for (int attempt = 0; attempt < 4; attempt++) {
            service.recordFailure(EMAIL);
        }

        assertFalse(service.isLocked(EMAIL));
        service.recordFailure(EMAIL);

        assertTrue(service.isLocked(EMAIL));
    }

    private static final class MutableClock extends Clock {

        private Instant currentInstant;
        private final ZoneId zone;

        private MutableClock(Instant initialInstant) {
            this(initialInstant, ZoneOffset.UTC);
        }

        private MutableClock(Instant initialInstant, ZoneId zone) {
            this.currentInstant = initialInstant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }
}
