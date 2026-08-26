package com.company.supportticketing.dto.response;
import com.company.supportticketing.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;
public record UserResponse(UUID id, String email, String name, UserRole role, Instant createdAt) { }
