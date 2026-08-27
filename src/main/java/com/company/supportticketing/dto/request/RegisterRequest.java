package com.company.supportticketing.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.company.supportticketing.domain.enums.UserRole;
public record RegisterRequest(@NotBlank @Email @Size(max=320) String email,
                              @NotBlank @Size(min=8,max=72) String password,
                              @NotBlank @Size(max=120) String name,
                              @NotNull UserRole role) { }
