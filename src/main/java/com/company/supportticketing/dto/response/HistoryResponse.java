package com.company.supportticketing.dto.response;
import com.company.supportticketing.domain.enums.*;
import java.time.Instant;
import java.util.UUID;
public record HistoryResponse(UUID id, UUID ticketId, HistoryEventType eventType, TicketStatus fromStatus,
                              TicketStatus toStatus, UUID fromAgentId, UUID toAgentId,
                              UUID changedBy, Instant changedAt, String note) { }
