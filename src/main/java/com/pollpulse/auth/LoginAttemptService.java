package com.pollpulse.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final long lockMinutes;

    public LoginAttemptService(
            Clock clock,
            @Value("${app.login.max-attempts:5}") int maxAttempts,
            @Value("${app.login.lock-minutes:15}") long lockMinutes) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    public void recordFailure(String email) {
        String key = normalize(email);
        Instant now = clock.instant();
        attempts.compute(key, (ignored, state) -> {
            if (state != null && state.lockedUntil() != null && now.isBefore(state.lockedUntil())) {
                return state;
            }

            int failureCount = state == null || state.lockedUntil() != null
                    ? 1
                    : state.failureCount() + 1;
            Instant lockedUntil = failureCount >= maxAttempts
                    ? now.plus(Duration.ofMinutes(lockMinutes))
                    : null;
            return new AttemptState(failureCount, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(normalize(email));
    }

    public boolean isLocked(String email) {
        String key = normalize(email);
        Instant now = clock.instant();
        AttemptState state = attempts.computeIfPresent(key, (ignored, current) -> {
            return current.lockedUntil() == null || now.isBefore(current.lockedUntil())
                    ? current
                    : null;
        });
        return state != null && state.lockedUntil() != null && now.isBefore(state.lockedUntil());
    }

    private String normalize(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptState(int failureCount, Instant lockedUntil) {
    }
}
