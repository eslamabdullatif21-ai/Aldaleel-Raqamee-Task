package com.company.supportticketing.dto.request;
import jakarta.validation.constraints.*;
public record AddCommentRequest(@NotBlank @Size(max=5000) String body) { }
