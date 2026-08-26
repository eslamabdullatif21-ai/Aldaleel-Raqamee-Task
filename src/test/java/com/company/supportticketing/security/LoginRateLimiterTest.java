package com.company.supportticketing.security;

import com.company.supportticketing.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {
    @Test void blocksAttemptsBeyondConfiguredLimitAndProvidesRetryDelay() {
        AtomicLong now = new AtomicLong(1_000);
        LoginRateLimiter limiter = new LoginRateLimiter(2, Duration.ofSeconds(60), now::get);

        limiter.check("127.0.0.1");
        limiter.check("127.0.0.1");
        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> limiter.check("127.0.0.1"));

        assertEquals(60, exception.retryAfterSeconds());
    }

    @Test void resetsTheWindowAndSeparatesClientAddresses() {
        AtomicLong now = new AtomicLong(1_000);
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofSeconds(10), now::get);

        limiter.check("client-a");
        limiter.check("client-b");
        assertThrows(RateLimitExceededException.class, () -> limiter.check("client-a"));
        now.addAndGet(10_000);

        assertDoesNotThrow(() -> limiter.check("client-a"));
    }

    @Test void concurrentRequestsCannotExceedTheLimit() {
        LoginRateLimiter limiter = new LoginRateLimiter(10, Duration.ofMinutes(1), () -> 1_000L);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();

        IntStream.range(0, 100).parallel().forEach(ignored -> {
            try {
                limiter.check("shared-client");
                allowed.incrementAndGet();
            } catch (RateLimitExceededException exception) {
                blocked.incrementAndGet();
            }
        });

        assertEquals(10, allowed.get());
        assertEquals(90, blocked.get());
    }

    @Test void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new LoginRateLimiter(0, Duration.ofMinutes(1), System::currentTimeMillis));
        assertThrows(IllegalArgumentException.class,
                () -> new LoginRateLimiter(1, Duration.ZERO, System::currentTimeMillis));
    }
}
