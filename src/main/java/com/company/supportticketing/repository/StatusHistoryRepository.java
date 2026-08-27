package com.company.supportticketing.repository;

import com.company.supportticketing.domain.entity.StatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import java.util.UUID;

/** Read-only status-history queries. Mutation is available only through StatusHistoryAppender. */
public interface StatusHistoryRepository extends Repository<StatusHistory, UUID> {
    Page<StatusHistory> findByTicketId(UUID ticketId, Pageable pageable);
}
