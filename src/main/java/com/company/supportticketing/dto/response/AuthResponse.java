package com.company.supportticketing.dto.response;
import java.time.Instant;
public record AuthResponse(String token, Instant expiresAt) { }
