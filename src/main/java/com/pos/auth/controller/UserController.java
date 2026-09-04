package com.pos.auth.controller;

import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.UserResponse;
import com.pos.auth.service.AuthService;
import com.pos.common.response.ApiResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response = authService.createUser(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User created successfully",
                        response
                )
        );
    }
}
