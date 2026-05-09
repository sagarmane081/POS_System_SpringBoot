package com.pos.auth.dto;

import com.pos.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String email;
    private Role role;
}