package com.pos.auth.controller;

import com.pos.auth.dto.AuthResponse;
import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.RefreshTokenRequest;
import com.pos.auth.dto.RegisterRequest;
import com.pos.auth.security.JwtProvider;
import com.pos.auth.service.AuthService;
import com.pos.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        AuthResponse response = authService.register(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User registered successfully",
                        response
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>>
    login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login successful",
                        response
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>>
    refresh(

            @RequestBody
            RefreshTokenRequest request
    ) {

        String email =

                jwtProvider
                        .extractUsername(
                                request
                                        .getRefreshToken()
                        );

        String token =

                jwtProvider
                        .generateToken(
                                email
                        );

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Token refreshed",

                        new AuthResponse(

                                token,

                                request
                                        .getRefreshToken()
                        )
                )
        );
    }
}