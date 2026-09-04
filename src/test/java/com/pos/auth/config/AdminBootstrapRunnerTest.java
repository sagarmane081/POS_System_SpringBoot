package com.pos.auth.config;

import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runner;

    private void configure(String email, String password) {

        runner = new AdminBootstrapRunner(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(runner, "bootstrapEmail", email);
        ReflectionTestUtils.setField(runner, "bootstrapPassword", password);
    }

    @Test
    void run_shouldSkip_whenEmailNotConfigured() throws Exception {

        configure("", "adminPass123");

        runner.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void run_shouldSkip_whenPasswordNotConfigured() throws Exception {

        configure("admin@example.com", "");

        runner.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void run_shouldSkip_whenAdminAlreadyExists() throws Exception {

        configure("admin@example.com", "adminPass123");

        when(userRepository.existsByRole(Role.ROLE_ADMIN)).thenReturn(true);

        runner.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_shouldSkip_whenEmailAlreadyBelongsToNonAdminUser() throws Exception {

        configure("admin@example.com", "adminPass123");

        when(userRepository.existsByRole(Role.ROLE_ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        runner.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_shouldCreateAdmin_whenNoAdminExistsAndEmailIsFree() throws Exception {

        configure("admin@example.com", "adminPass123");

        when(userRepository.existsByRole(Role.ROLE_ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("adminPass123")).thenReturn("encoded-password");

        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }
}
