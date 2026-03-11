package com.pos.service;
import com.pos.exception.UserException;
import com.pos.model.User;
import java.util.List;

public interface UserService {

    User getUserFromJwtToken(String jwtToken) throws UserException;
    User getCurrentUser() throws UserException;
    User getUserByEmail(String email) throws UserException;
    User getUserById(long id) throws UserException;
    List<User> getAllUsers();

}
