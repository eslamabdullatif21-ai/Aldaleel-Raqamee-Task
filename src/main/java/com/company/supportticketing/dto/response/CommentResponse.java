package com.company.supportticketing.dto.response;
import com.company.supportticketing.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;
public record CommentResponse(UUID id, UUID ticketId, UUID authorId, String authorName, UserRole authorRole, String body, Instant createdAt) { }
