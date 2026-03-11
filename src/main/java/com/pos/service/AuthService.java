package com.pos.service;

import com.pos.exception.UserException;
import com.pos.payload.dto.UserDto;
import com.pos.payload.response.AuthResponse;

public interface AuthService {

    AuthResponse signup(UserDto userDto) throws UserException;
    AuthResponse login(UserDto userDto) throws Exception;
}
