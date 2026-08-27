package com.company.supportticketing.dto.response;
import java.time.Instant;
import java.util.UUID;

import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
import com.company.supportticketing.domain.enums.TicketStatus;
public record TicketResponse(UUID id, String title, String description, TicketCategory category,
                             TicketPriority priority, TicketStatus status, UUID customerId,
                             UUID assignedAgentId, Instant createdAt, Instant updatedAt) { }
