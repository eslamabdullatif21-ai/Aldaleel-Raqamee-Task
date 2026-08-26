package com.company.supportticketing.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(Instant timestamp, int status, String error, String message, String path,
                               List<FieldErrorResponse> fieldErrors) { }
