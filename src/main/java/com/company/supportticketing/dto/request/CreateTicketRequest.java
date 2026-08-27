package com.company.supportticketing.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.company.supportticketing.domain.enums.TicketCategory;
import com.company.supportticketing.domain.enums.TicketPriority;
public record CreateTicketRequest(@NotBlank @Size(max=200) String title,
                                  @NotBlank @Size(max=5000) String description,
                                  @NotNull TicketCategory category,
                                  TicketPriority priority) { }
