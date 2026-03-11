package com.pos.payload.response;

import com.pos.payload.dto.UserDto;
import lombok.Data;

@Data
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;
}
