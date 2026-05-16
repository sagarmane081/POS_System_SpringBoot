package com.pos.auth.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;

    private String refreshToken;
}