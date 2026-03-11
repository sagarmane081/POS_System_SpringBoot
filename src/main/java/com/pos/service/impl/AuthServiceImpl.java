package com.pos.service.impl;

import com.pos.configuration.JwtProvider;
import com.pos.domain.UserRole;
import com.pos.exception.UserException;
import com.pos.mapper.UserMapper;
import com.pos.model.User;
import com.pos.payload.dto.UserDto;
import com.pos.payload.response.AuthResponse;
import com.pos.repository.UserRepository;
import com.pos.service.AuthService;
import com.pos.service.CustomUserImplementation;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserImplementation customUserImplementation;

    @Override
    public AuthResponse signup(UserDto userDto) throws UserException {

        User user = userRepository.findByEmail(userDto.getEmail());

        if (user != null) {
            throw new UserException("E-mail ID is already registered. Please try another e-mail ID.");
        }

        if (userDto.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new UserException("You are not allowed to perform this action.");
        }

        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser.setRole(userDto.getRole());
        newUser.setFullName(userDto.getFullName());
        newUser.setPhoneNumber(userDto.getPhoneNumber());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLoginAt(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDto.getEmail(),
                        userDto.getPassword()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("User registered successfully");
        authResponse.setUser(UserMapper.toDTO(savedUser));

        return authResponse;
    }

    @Override
    public AuthResponse login(UserDto userDto) throws Exception {

        String email = userDto.getEmail();
        String password = userDto.getPassword();

        Authentication authentication = authenticate(email, password);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication);

        User user = userRepository.findByEmail(email);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Logged in successfully");
        authResponse.setUser(UserMapper.toDTO(user));

        return authResponse;
    }

    private Authentication authenticate(String email, String password) throws Exception {

        UserDetails userDetails = customUserImplementation.loadUserByUsername(email);

        if (userDetails == null) {
            throw new Exception("Email does not exist: " + email);
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new Exception("Invalid password");
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                userDetails.getAuthorities()
        );
    }
}