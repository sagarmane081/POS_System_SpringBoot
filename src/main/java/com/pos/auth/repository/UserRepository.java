package com.pos.auth.repository;

import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);
}