package com.company.supportticketing.dto.request;
import com.company.supportticketing.domain.enums.TicketPriority;
import jakarta.validation.constraints.NotNull;
public record UpdatePriorityRequest(@NotNull TicketPriority priority) { }
