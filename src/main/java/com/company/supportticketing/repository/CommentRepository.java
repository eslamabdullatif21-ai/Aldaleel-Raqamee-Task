package com.company.supportticketing.repository;

import com.company.supportticketing.domain.entity.TicketComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<TicketComment, UUID> {
    Page<TicketComment> findByTicketId(UUID ticketId, Pageable pageable);
}
