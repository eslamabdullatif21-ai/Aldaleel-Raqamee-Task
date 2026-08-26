package com.company.supportticketing.mapper;

import com.company.supportticketing.domain.entity.*;
import com.company.supportticketing.dto.response.*;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public TicketResponse toResponse(Ticket t) {
        return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(), t.getCategory(), t.getPriority(),
                t.getStatus(), t.getCustomer().getId(), t.getAssignedAgent() == null ? null : t.getAssignedAgent().getId(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
    public CommentResponse toResponse(TicketComment c) {
        return new CommentResponse(c.getId(), c.getTicket().getId(), c.getAuthor().getId(), c.getAuthor().getName(),
                c.getAuthor().getRole(), c.getBody(), c.getCreatedAt());
    }
    public HistoryResponse toResponse(StatusHistory h) {
        return new HistoryResponse(h.getId(), h.getTicket().getId(), h.getEventType(), h.getFromStatus(), h.getToStatus(),
                h.getFromAgentId(), h.getToAgentId(), h.getChangedBy().getId(), h.getChangedAt(), h.getNote());
    }
}
