package com.company.supportticketing.dto.request;
import com.company.supportticketing.domain.enums.UserRole;
import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank @Email @Size(max=320) String email,
                              @NotBlank @Size(min=8,max=72) String password,
                              @NotBlank @Size(max=120) String name,
                              @NotNull UserRole role) { }
