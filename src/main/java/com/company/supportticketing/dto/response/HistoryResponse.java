package com.company.supportticketing.dto.response;
import java.time.Instant;
import java.util.UUID;

import com.company.supportticketing.domain.enums.HistoryEventType;
import com.company.supportticketing.domain.enums.TicketStatus;
public record HistoryResponse(UUID id, UUID ticketId, HistoryEventType eventType, TicketStatus fromStatus,
                              TicketStatus toStatus, UUID fromAgentId, UUID toAgentId,
                              UUID changedBy, Instant changedAt, String note) { }
