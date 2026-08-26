package com.company.supportticketing.exception;

import com.company.supportticketing.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

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
        return error(HttpStatus.BAD_REQUEST, "Malformed request body or invalid enum value", request, null);
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
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception ex, HttpServletRequest request) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null); }
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request, java.util.List<FieldErrorResponse> fields) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields));
    }
}
