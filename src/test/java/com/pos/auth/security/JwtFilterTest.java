package com.pos.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void clearSecurityContext() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldContinueChainWithoutAuth_whenAuthHeaderMissing() throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtProvider, userDetailsService);
    }

    @Test
    void doFilterInternal_shouldContinueChainWithoutAuth_whenHeaderNotBearer() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtProvider, userDetailsService);
    }

    @Test
    void doFilterInternal_shouldContinueChainWithoutAuth_whenTokenInvalid() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtProvider.validateToken("bad-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenTokenValid() throws Exception {

        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.ROLE_ADMIN)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtProvider.validateToken("good-token")).thenReturn(true);
        when(jwtProvider.extractEmail("good-token")).thenReturn("john@example.com");
        when(userDetailsService.loadUserByUsername("john@example.com"))
                .thenReturn(new CustomUserDetails(user));

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("john@example.com");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");

        verify(filterChain).doFilter(request, response);
    }
}
