package com.pos.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.common.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * In-memory fixed-window rate limiter for the login/register endpoints,
 * keyed by client IP. Single-instance only; a multi-node deployment would
 * need a shared store (e.g. Redis) instead of the in-process map below.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${rate-limit.auth.max-requests:5}") int maxRequests,
            @Value("${rate-limit.auth.window-seconds:60}") long windowSeconds
    ) {

        this(objectMapper, maxRequests, windowSeconds, System::currentTimeMillis);
    }

    RateLimitingFilter(
            ObjectMapper objectMapper,
            int maxRequests,
            long windowSeconds,
            LongSupplier clock
    ) {

        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!LIMITED_PATHS.contains(path)) {

            filterChain.doFilter(request, response);
            return;
        }

        String key = path + "|" + clientIp(request);

        if (isRateLimited(key)) {

            respondTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key) {

        long now = clock.getAsLong();

        Window window = windows.compute(key, (k, existing) -> {

            if (existing == null || now - existing.windowStart >= windowMillis) {

                return new Window(now);
            }

            existing.count.incrementAndGet();
            return existing;
        });

        return window.count.get() > maxRequests;
    }

    private String clientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {

            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        new ApiResponse<>(
                                false,
                                "Too many requests. Please try again later.",
                                null
                        )
                )
        );
    }

    private static final class Window {

        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
