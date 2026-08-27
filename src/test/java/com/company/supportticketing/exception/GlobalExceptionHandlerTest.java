package com.company.supportticketing.exception;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.company.supportticketing.domain.enums.TicketStatus;
import com.company.supportticketing.dto.response.ApiErrorResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/tickets");
    }

    @Test
    void optimisticLock_returns409Conflict() {
        request.setRequestURI("/api/tickets/123/status");
        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("Ticket", UUID.randomUUID());

        ResponseEntity<ApiErrorResponse> response = handler.optimisticLock(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("This ticket was modified by someone else. Please retry your request.", response.getBody().message());
        assertEquals("/api/tickets/123/status", response.getBody().path());
    }

    @Test
    void rateLimited_returns429TooManyRequests_withRetryAfterHeader() {
        request.setRequestURI("/api/auth/login");
        ResponseEntity<ApiErrorResponse> response = handler.rateLimited(new RateLimitExceededException(42), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("42", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertNotNull(response.getBody());
        assertEquals(429, response.getBody().status());
        assertEquals("/api/auth/login", response.getBody().path());
    }

    @Test
    void forbidden_returns403_forPermissionDeniedException() {
        ResponseEntity<ApiErrorResponse> response = handler.forbidden(new PermissionDeniedException("You do not have access"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("You do not have access", response.getBody().message());
    }

    @Test
    void forbidden_returns403_forAccessDeniedException() {
        ResponseEntity<ApiErrorResponse> response = handler.forbidden(new AccessDeniedException("Access denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Access denied", response.getBody().message());
    }

    @Test
    void notFound_returns404_forTicketAndUserNotFound() {
        UUID id = UUID.randomUUID();
        ResponseEntity<ApiErrorResponse> respTicket = handler.notFound(new TicketNotFoundException(id), request);
        assertEquals(HttpStatus.NOT_FOUND, respTicket.getStatusCode());
        assertEquals(404, respTicket.getBody().status());

        ResponseEntity<ApiErrorResponse> respUser = handler.notFound(new UserNotFoundException(id), request);
        assertEquals(HttpStatus.NOT_FOUND, respUser.getStatusCode());
        assertEquals(404, respUser.getBody().status());
    }

    @Test
    void conflict_returns409_forInvalidTransitionAndDuplicateEmail() {
        ResponseEntity<ApiErrorResponse> respTransition = handler.conflict(new InvalidTransitionException(TicketStatus.OPEN, TicketStatus.RESOLVED), request);
        assertEquals(HttpStatus.CONFLICT, respTransition.getStatusCode());
        assertEquals(409, respTransition.getBody().status());
        assertEquals("Cannot transition ticket from OPEN to RESOLVED", respTransition.getBody().message());

        ResponseEntity<ApiErrorResponse> respEmail = handler.conflict(new DuplicateEmailException(), request);
        assertEquals(HttpStatus.CONFLICT, respEmail.getStatusCode());
        assertEquals(409, respEmail.getBody().status());
        assertEquals("An account with this email already exists", respEmail.getBody().message());

        ResponseEntity<ApiErrorResponse> respDataIntegrity = handler.conflict(new DataIntegrityViolationException("constraint error"), request);
        assertEquals(HttpStatus.CONFLICT, respDataIntegrity.getStatusCode());
        assertEquals(409, respDataIntegrity.getBody().status());
        assertEquals("The request conflicts with existing data", respDataIntegrity.getBody().message());
    }

    @Test
    void unauthorized_returns401_forBadCredentials() {
        ResponseEntity<ApiErrorResponse> response = handler.unauthorized(new BadCredentialsException("Bad"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Invalid email or password", response.getBody().message());
    }

    @Test
    void unknownRoute_returns404_forNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/does-not-exist");
        ResponseEntity<ApiErrorResponse> response = handler.unknownRoute(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Endpoint not found", response.getBody().message());
    }

    @Test
    void unsupportedMethod_returns405_forMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PUT");
        ResponseEntity<ApiErrorResponse> response = handler.unsupportedMethod(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().status());
        assertEquals("HTTP method is not supported for this endpoint", response.getBody().message());
    }

    @Test
    void unexpected_returns500_forGeneralException() {
        ResponseEntity<ApiErrorResponse> response = handler.unexpected(new RuntimeException("Crash"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertNull(response.getBody().fieldErrors());
    }
}
