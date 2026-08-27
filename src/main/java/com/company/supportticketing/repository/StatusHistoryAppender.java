package com.company.supportticketing.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.company.supportticketing.domain.entity.StatusHistory;

/** Insert-only persistence boundary for immutable status-history events. */
@Repository
@RequiredArgsConstructor
public class StatusHistoryAppender {
    private final EntityManager entityManager;

    public StatusHistory append(StatusHistory event) {
        if (event.getId() != null) {
            throw new IllegalArgumentException("Status history events cannot be updated");
        }
        entityManager.persist(event);
        return event;
    }
}
