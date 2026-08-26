package com.company.supportticketing.security;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUserCacheTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AtomicLong clock = new AtomicLong();
    private final AppUser persisted = AppUser.builder()
            .id(UUID.randomUUID()).email("Customer@Example.com").passwordHash("must-not-be-cached")
            .name("Customer").role(UserRole.CUSTOMER).build();

    @Test void reusesPasswordFreeSnapshotWithinTtl() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(persisted));
        JwtUserCache cache = new JwtUserCache(users, Duration.ofSeconds(60), clock::get);

        AppUser first = cache.get("Customer@Example.com");
        AppUser second = cache.get("customer@example.com");

        assertSame(first, second);
        assertNull(first.getPasswordHash());
        assertEquals(persisted.getId(), first.getId());
        verify(users, times(1)).findByEmailIgnoreCase(anyString());
    }

    @Test void reloadsAfterTtlAndCanBeExplicitlyEvicted() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(persisted));
        JwtUserCache cache = new JwtUserCache(users, Duration.ofSeconds(60), clock::get);

        cache.get(persisted.getEmail());
        clock.set(Duration.ofSeconds(61).toNanos());
        cache.get(persisted.getEmail());
        cache.evict(persisted.getEmail());
        cache.get(persisted.getEmail());

        verify(users, times(3)).findByEmailIgnoreCase(anyString());
    }

    @Test void missingUsersAreNotCached() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        JwtUserCache cache = new JwtUserCache(users, Duration.ofSeconds(60), clock::get);

        assertThrows(UsernameNotFoundException.class, () -> cache.get("missing@example.com"));
        assertThrows(UsernameNotFoundException.class, () -> cache.get("missing@example.com"));
        verify(users, times(2)).findByEmailIgnoreCase(anyString());
    }

    @Test void rejectsNonPositiveTtl() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtUserCache(users, Duration.ZERO, clock::get));
    }
}
