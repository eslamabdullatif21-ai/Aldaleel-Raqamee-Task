package com.company.supportticketing.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test void rateLimitResponseUsesSharedErrorShapeAndRetryAfterHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        var response = new GlobalExceptionHandler().rateLimited(new RateLimitExceededException(42), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("42", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertNotNull(response.getBody());
        assertEquals(429, response.getBody().status());
        assertEquals("/api/auth/login", response.getBody().path());
    }
}
