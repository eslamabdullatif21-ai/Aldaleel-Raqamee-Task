package com.company.supportticketing.repository;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.supportticketing.domain.entity.StatusHistory;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatusHistoryAppenderTest {
    @Mock EntityManager entityManager;

    @Test
    void append_persistsNewEvent() {
        StatusHistory event = StatusHistory.builder().build();
        StatusHistoryAppender appender = new StatusHistoryAppender(entityManager);

        StatusHistory result = appender.append(event);

        assertSame(event, result);
        verify(entityManager).persist(event);
    }

    @Test
    void append_rejectsExistingEvent() {
        StatusHistory event = StatusHistory.builder().id(UUID.randomUUID()).build();
        StatusHistoryAppender appender = new StatusHistoryAppender(entityManager);

        assertThrows(IllegalArgumentException.class, () -> appender.append(event));
        verify(entityManager, never()).persist(event);
    }
}
