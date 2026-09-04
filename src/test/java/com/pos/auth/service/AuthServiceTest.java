package com.pos.auth.service;

import com.pos.auth.dto.AuthResponse;
import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.RegisterRequest;
import com.pos.auth.dto.UserResponse;
import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;
import com.pos.auth.security.JwtProvider;
import com.pos.common.exception.DuplicateResourceException;
import com.pos.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldThrowDuplicateResourceException_whenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldCreateUserAndReturnTokens_whenEmailIsNew() {

        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtProvider.generateToken("john@example.com")).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken("john@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("John Doe");
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void register_shouldNormalizeEmail_beforeCheckingDuplicatesAndStoring() {

        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("  John@Example.COM  ");
        request.setPassword("password123");

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtProvider.generateToken("john@example.com")).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken("john@example.com")).thenReturn("refresh-token");

        authService.register(request);

        verify(userRepository).existsByEmailIgnoreCase("john@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void login_shouldAuthenticateAndReturnTokens_whenCredentialsAreValid() {

        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(jwtProvider.generateToken("john@example.com")).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken("john@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")
        );
    }

    @Test
    void login_shouldPropagateAuthenticationException_whenCredentialsAreInvalid() {

        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void login_shouldThrowResourceNotFoundException_whenUserMissingAfterAuthentication() {

        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void login_shouldNormalizeEmail_beforeAuthenticatingAndLookingUpUser() {

        LoginRequest request = new LoginRequest();
        request.setEmail("  John@Example.COM  ");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(jwtProvider.generateToken("john@example.com")).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken("john@example.com")).thenReturn("refresh-token");

        authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123")
        );
        verify(userRepository).findByEmailIgnoreCase("john@example.com");
    }

    @Test
    void createUser_shouldThrowDuplicateResourceException_whenEmailAlreadyExists() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Cashier One");
        request.setEmail("cashier@example.com");
        request.setPassword("password123");
        request.setRole(Role.ROLE_CASHIER);

        when(userRepository.existsByEmailIgnoreCase("cashier@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_shouldSaveWithRequestedRoleAndReturnUserResponse() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Cashier One");
        request.setEmail("cashier@example.com");
        request.setPassword("password123");
        request.setRole(Role.ROLE_CASHIER);

        when(userRepository.existsByEmailIgnoreCase("cashier@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });

        UserResponse response = authService.createUser(request);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Cashier One");
        assertThat(response.getEmail()).isEqualTo("cashier@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_CASHIER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void createUser_shouldNormalizeEmail_beforeCheckingDuplicatesAndStoring() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Cashier One");
        request.setEmail("  Cashier@Example.COM  ");
        request.setPassword("password123");
        request.setRole(Role.ROLE_CASHIER);

        when(userRepository.existsByEmailIgnoreCase("cashier@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.createUser(request);

        assertThat(response.getEmail()).isEqualTo("cashier@example.com");
        verify(userRepository).existsByEmailIgnoreCase("cashier@example.com");
    }
}
