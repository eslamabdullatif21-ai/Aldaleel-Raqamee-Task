package com.company.supportticketing.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AddCommentRequest(@NotBlank @Size(max=5000) String body) { }
