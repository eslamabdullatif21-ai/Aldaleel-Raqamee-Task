package com.company.supportticketing.dto.request;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank @Email @Size(max=320) String email, @NotBlank @Size(max=72) String password) { }
