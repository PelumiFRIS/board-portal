package com.fris.boardportal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Without this, Spring Security's default entry point (Http403ForbiddenEntryPoint,
 * used automatically since this app has no formLogin/httpBasic) returns 403 for a
 * missing, expired, or invalid JWT — indistinguishable from a genuine @PreAuthorize
 * role failure. This returns 401 for "not authenticated at all" so the frontend can
 * tell an expired session apart from a real permission error.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"Your session has expired. Please log in again.","fieldErrors":[]}"""
                .formatted(Instant.now());
        response.getWriter().write(body);
    }
}
