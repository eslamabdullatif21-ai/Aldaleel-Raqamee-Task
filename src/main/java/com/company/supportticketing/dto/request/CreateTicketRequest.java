package com.company.supportticketing.dto.request;
import com.company.supportticketing.domain.enums.*;
import jakarta.validation.constraints.*;
public record CreateTicketRequest(@NotBlank @Size(max=200) String title,
                                  @NotBlank @Size(max=5000) String description,
                                  @NotNull TicketCategory category,
                                  TicketPriority priority) { }
