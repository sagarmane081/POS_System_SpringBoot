package com.pos.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final AtomicLong now = new AtomicLong(0L);

    private RateLimitingFilter filterWithLimit(int maxRequests) {

        return new RateLimitingFilter(
                new ObjectMapper(),
                maxRequests,
                60,
                now::get
        );
    }

    @BeforeEach
    void resetClock() {

        now.set(0L);
    }

    @Test
    void doFilterInternal_shouldPassThrough_forPathNotRateLimited() throws Exception {

        RateLimitingFilter filter = filterWithLimit(1);

        when(request.getRequestURI()).thenReturn("/api/products");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_shouldAllowRequestsWithinLimit() throws Exception {

        RateLimitingFilter filter = filterWithLimit(3);

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_shouldReturn429_onceLimitExceeded() throws Exception {

        RateLimitingFilter filter = filterWithLimit(2);

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(429);
        assertThat(body.toString()).contains("Too many requests");
    }

    @Test
    void doFilterInternal_shouldTrackDifferentClientIps_independently() throws Exception {

        RateLimitingFilter filter = filterWithLimit(1);

        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1", "10.0.0.2");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_shouldTrackDifferentLimitedPaths_independently() throws Exception {

        RateLimitingFilter filter = filterWithLimit(1);

        when(request.getRequestURI()).thenReturn("/api/auth/login", "/api/auth/register");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_shouldResetCount_afterWindowExpires() throws Exception {

        RateLimitingFilter filter = filterWithLimit(1);

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        now.set(61_000L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_shouldPreferForwardedForHeader_overRemoteAddr() throws Exception {

        RateLimitingFilter filter = filterWithLimit(1);

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(request, never()).getRemoteAddr();
    }
}
