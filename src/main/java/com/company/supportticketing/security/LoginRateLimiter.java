package com.company.supportticketing.security;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.company.supportticketing.exception.RateLimitExceededException;

@Component
public class LoginRateLimiter {
    private static final long CLEANUP_INTERVAL = 256;

    private final ConcurrentMap<String, AttemptWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final int maxAttempts;
    private final long windowMillis;
    private final LongSupplier currentTimeMillis;

    @Autowired
    public LoginRateLimiter(
            @Value("${app.rate-limit.login.max-attempts:10}") int maxAttempts,
            @Value("${app.rate-limit.login.window:PT1M}") Duration window) {
        this(maxAttempts, window, System::currentTimeMillis);
    }

    LoginRateLimiter(int maxAttempts, Duration window, LongSupplier currentTimeMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Login rate limit must allow at least one attempt");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Login rate-limit window must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = window.toMillis();
        this.currentTimeMillis = Objects.requireNonNull(currentTimeMillis);
    }

    public void check(String clientAddress) {
        long now = currentTimeMillis.getAsLong();
        String key = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        AttemptWindow window = windows.compute(key, (ignored, current) ->
                current == null || now >= current.resetAtMillis()
                        ? new AttemptWindow(1, now + windowMillis)
                        : new AttemptWindow(current.attempts() + 1, current.resetAtMillis()));

        if (operations.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            windows.entrySet().removeIf(entry -> entry.getValue().resetAtMillis() <= now);
        }
        if (window.attempts() > maxAttempts) {
            long retryAfter = Math.max(1, (window.resetAtMillis() - now + 999) / 1000);
            throw new RateLimitExceededException(retryAfter);
        }
    }

    private record AttemptWindow(int attempts, long resetAtMillis) {}
}
