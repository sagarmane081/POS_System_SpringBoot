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
import com.pos.auth.util.EmailNormalizer;
import com.pos.common.exception.DuplicateResourceException;
import com.pos.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(
            RegisterRequest request
    ) {

        String normalizedEmail =
                EmailNormalizer.normalize(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {

            throw new DuplicateResourceException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(normalizedEmail)
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);

        String token =
                jwtProvider.generateToken(
                        user.getEmail()
                );

        String refreshToken =
                jwtProvider.generateRefreshToken(
                        user.getEmail()
                );

        return new AuthResponse(
                token,
                refreshToken
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        String normalizedEmail =
                EmailNormalizer.normalize(request.getEmail());

        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(

                        normalizedEmail,

                        request.getPassword()
                )
        );

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .orElseThrow(() ->

                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        String token =
                jwtProvider.generateToken(
                        user.getEmail()
                );

        String refreshToken =
                jwtProvider.generateRefreshToken(
                        user.getEmail()
                );

        return new AuthResponse(
                token,
                refreshToken
        );
    }

    public UserResponse createUser(
            CreateUserRequest request
    ) {

        String normalizedEmail =
                EmailNormalizer.normalize(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {

            throw new DuplicateResourceException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(normalizedEmail)
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);

        log.info(
                "User created by admin: {} ({})",
                savedUser.getEmail(),
                savedUser.getRole()
        );

        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }
}