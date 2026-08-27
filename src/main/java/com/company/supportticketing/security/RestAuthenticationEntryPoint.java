package com.company.supportticketing.security;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.company.supportticketing.dto.response.ApiErrorResponse;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    @Override
    public void commence(
            HttpServletRequest req,
            HttpServletResponse res,
            AuthenticationException ex) throws IOException {
        res.setStatus(401);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(
                res.getOutputStream(),
                new ApiErrorResponse(
                        Instant.now(),
                        401,
                        "Unauthorized",
                        "Authentication is required",
                        req.getRequestURI(),
                        null));
    }
}
