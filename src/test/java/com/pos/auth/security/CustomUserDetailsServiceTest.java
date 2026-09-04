package com.pos.auth.security;

import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenFound() {

        service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.ROLE_ADMIN)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("john@example.com");

        assertThat(details.getUsername()).isEqualTo("john@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void loadUserByUsername_shouldNormalizeEmail_beforeLookup() {

        service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));

        service.loadUserByUsername("  John@Example.COM  ");

        verify(userRepository).findByEmailIgnoreCase("john@example.com");
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenMissing() {

        service = new CustomUserDetailsService(userRepository);

        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
