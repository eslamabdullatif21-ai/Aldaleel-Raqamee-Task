package com.company.supportticketing.dto.response;
import com.company.supportticketing.domain.enums.*;
import java.time.Instant;
import java.util.UUID;
public record TicketResponse(UUID id, String title, String description, TicketCategory category,
                             TicketPriority priority, TicketStatus status, UUID customerId,
                             UUID assignedAgentId, Instant createdAt, Instant updatedAt) { }
