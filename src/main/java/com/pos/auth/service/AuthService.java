package com.pos.auth.service;

import com.pos.auth.dto.AuthResponse;
import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.RegisterRequest;
import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;
import com.pos.auth.security.JwtProvider;

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

        if (userRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
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

        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(

                        request.getEmail(),

                        request.getPassword()
                )
        );

        User user =
                userRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(() ->

                                new RuntimeException(
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
}