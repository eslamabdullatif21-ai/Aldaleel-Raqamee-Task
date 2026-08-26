package com.company.supportticketing.repository;

import com.company.supportticketing.domain.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CommentRepository extends JpaRepository<TicketComment, UUID> {
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
