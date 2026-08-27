package com.company.supportticketing.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.company.supportticketing.domain.enums.TicketStatus;
public record UpdateStatusRequest(@NotNull TicketStatus status, @Size(max=1000) String note) { }
