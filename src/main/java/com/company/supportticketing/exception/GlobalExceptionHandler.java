package com.company.supportticketing.exception;

import com.company.supportticketing.dto.response.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorResponse(e.getField(), e.getDefaultMessage())).toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fields);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> malformed(HttpMessageNotReadableException ex, HttpServletRequest request) {
        InvalidFormatException format = findCause(ex, InvalidFormatException.class);
        if (format != null && format.getTargetType().isEnum() && !format.getPath().isEmpty()) {
            String field = format.getPath().get(format.getPath().size() - 1).getFieldName();
            String values = String.join(", ", Arrays.stream(format.getTargetType().getEnumConstants())
                    .map(Object::toString).toList());
            return error(HttpStatus.BAD_REQUEST, "Validation failed", request,
                    java.util.List.of(new FieldErrorResponse(field, "must be one of: " + values)));
        }
        return error(HttpStatus.BAD_REQUEST, "Malformed request body or invalid enum value", request, null);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> invalidPathValue(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for " + ex.getName(), request, null);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> unknownRoute(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Endpoint not found", request, null);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> unsupportedMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint", request, null);
    }
    @ExceptionHandler({PermissionDeniedException.class, org.springframework.security.access.AccessDeniedException.class})
    ResponseEntity<ApiErrorResponse> forbidden(Exception ex, HttpServletRequest request) { return error(HttpStatus.FORBIDDEN, ex.getMessage(), request, null); }
    @ExceptionHandler({TicketNotFoundException.class, UserNotFoundException.class})
    ResponseEntity<ApiErrorResponse> notFound(Exception ex, HttpServletRequest request) { return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, null); }
    @ExceptionHandler({InvalidTransitionException.class, DuplicateEmailException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiErrorResponse> conflict(Exception ex, HttpServletRequest request) {
        String message = ex instanceof DataIntegrityViolationException ? "The request conflicts with existing data" : ex.getMessage();
        return error(HttpStatus.CONFLICT, message, request, null);
    }
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiErrorResponse> unauthorized(Exception ex, HttpServletRequest request) { return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", request, null); }
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiErrorResponse> rateLimited(RateLimitExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
                .body(response(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, null));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception ex, HttpServletRequest request) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null); }
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request, java.util.List<FieldErrorResponse> fields) {
        return ResponseEntity.status(status).body(response(status, message, request, fields));
    }
    private ApiErrorResponse response(HttpStatus status, String message, HttpServletRequest request, java.util.List<FieldErrorResponse> fields) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields);
    }
    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return type.cast(current);
            current = current.getCause();
        }
        return null;
    }
}
