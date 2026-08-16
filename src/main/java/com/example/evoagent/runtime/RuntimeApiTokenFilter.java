package com.example.evoagent.runtime;

import com.example.evoagent.common.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
public class RuntimeApiTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Runtime-Token";

    private final ObjectMapper objectMapper;

    public RuntimeApiTokenFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/runtime/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String configuredToken = System.getenv("RUNTIME_API_TOKEN");
        if (configuredToken == null || configuredToken.isBlank()) {
            if (isDirectLocalRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeUnauthorized(response, request, "Missing RUNTIME_API_TOKEN configuration for protected runtime API");
            return;
        }

        String requestToken = request.getHeader(HEADER_NAME);
        if (!configuredToken.equals(requestToken)) {
            writeUnauthorized(response, request, "Invalid or missing runtime API token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isDirectLocalRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean localAddress = "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr);
        boolean forwarded = request.getHeader("X-Forwarded-For") != null
                || request.getHeader("CF-Connecting-IP") != null;
        return localAddress && !forwarded;
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            HttpServletRequest request,
            String message
    ) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
