package com.company.supportticketing.security;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

@Component
public class JwtUserCache {
    private final UserRepository users;
    private final long ttlNanos;
    private final LongSupplier nanoTime;
    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    @Autowired
    public JwtUserCache(UserRepository users,
                        @Value("${app.jwt.user-cache-ttl:PT1M}") Duration ttl) {
        this(users, ttl, System::nanoTime);
    }

    JwtUserCache(UserRepository users, Duration ttl, LongSupplier nanoTime) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("JWT user cache TTL must be positive");
        }
        this.users = users;
        this.ttlNanos = ttl.toNanos();
        this.nanoTime = nanoTime;
    }

    public AppUser get(String email) {
        String key = email.trim().toLowerCase(Locale.ROOT);
        long now = nanoTime.getAsLong();
        CacheEntry current = entries.get(key);
        if (current != null && current.expiresAtNanos() > now) {
            cleanExpiredOccasionally(now);
            return current.user();
        }

        CacheEntry loaded = entries.compute(key, (ignored, existing) -> {
            long loadTime = nanoTime.getAsLong();
            if (existing != null && existing.expiresAtNanos() > loadTime) return existing;
            AppUser user = users.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return new CacheEntry(snapshot(user), loadTime + ttlNanos);
        });
        cleanExpiredOccasionally(now);
        return loaded.user();
    }

    public void evict(String email) {
        entries.remove(email.trim().toLowerCase(Locale.ROOT));
    }

    private AppUser snapshot(AppUser user) {
        return AppUser.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void cleanExpiredOccasionally(long now) {
        if ((requestCount.incrementAndGet() & 255) == 0) {
            entries.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() <= now);
        }
    }

    private record CacheEntry(AppUser user, long expiresAtNanos) { }
}
