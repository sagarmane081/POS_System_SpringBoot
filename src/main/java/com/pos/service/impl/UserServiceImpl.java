package com.pos.service.impl;

import com.pos.configuration.JwtProvider;
import com.pos.exception.UserException;
import com.pos.model.User;
import com.pos.repository.UserRepository;
import com.pos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public User getUserFromJwtToken(String token) throws UserException {

        String email = jwtProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(email);

        if (user == null) {

            throw new UserException("Invalid username");
        }

        return user;
    }

    @Override
    public User getCurrentUser() throws UserException {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {

            throw new UserException("User not found");
        }

        return user;
    }

    @Override
    public User getUserByEmail(String email) throws UserException {

        User user = userRepository.findByEmail(email);

        if (user == null) {

            throw new UserException("User not found");
        }

        return user;
    }

    @Override
    public User getUserById(long id) throws UserException {
        return userRepository.findById(id).orElseThrow(
                () -> new UserException("User not found")
        );
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
