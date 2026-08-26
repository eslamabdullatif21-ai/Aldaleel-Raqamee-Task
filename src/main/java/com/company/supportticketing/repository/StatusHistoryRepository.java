package com.company.supportticketing.repository;

import com.company.supportticketing.domain.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findByTicketIdOrderByChangedAtAsc(UUID ticketId);
}
