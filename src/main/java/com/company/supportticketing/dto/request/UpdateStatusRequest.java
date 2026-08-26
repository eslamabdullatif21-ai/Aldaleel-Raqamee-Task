package com.company.supportticketing.dto.request;
import com.company.supportticketing.domain.enums.TicketStatus;
import jakarta.validation.constraints.*;
public record UpdateStatusRequest(@NotNull TicketStatus status, @Size(max=1000) String note) { }
