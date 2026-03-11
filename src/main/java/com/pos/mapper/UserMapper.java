package com.pos.mapper;

import com.pos.model.User;
import com.pos.payload.dto.UserDto;

public class UserMapper {

    public static UserDto toDTO(User savedUser){

        UserDto userDto = new UserDto();

        userDto.setId(savedUser.getId());
        userDto.setFullName(savedUser.getFullName());
        userDto.setEmail(savedUser.getEmail());
        userDto.setPhoneNumber(savedUser.getPhoneNumber());
        userDto.setRole(savedUser.getRole());
        userDto.setCreatedAt(savedUser.getCreatedAt());
        userDto.setUpdatedAt(savedUser.getUpdatedAt());
        userDto.setLastLoginAt(savedUser.getLastLoginAt());

        return userDto;
    }
}