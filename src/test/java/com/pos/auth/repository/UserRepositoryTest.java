package com.pos.auth.repository;

import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user(String email) {

        return user(email, Role.ROLE_USER);
    }

    private User user(String email, Role role) {

        return User.builder()
                .name("John Doe")
                .email(email)
                .password("encoded-password")
                .role(role)
                .build();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenUserExists() {

        userRepository.save(user("john@example.com"));

        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenUserMissing() {

        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
    }

    @Test
    void findByEmail_shouldReturnUser_whenExists() {

        userRepository.save(user("john@example.com"));

        Optional<User> found = userRepository.findByEmail("john@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenMissing() {

        assertThat(userRepository.findByEmail("ghost@example.com")).isEmpty();
    }

    @Test
    void existsByRole_shouldReturnTrue_whenAdminExists() {

        userRepository.save(user("admin@example.com", Role.ROLE_ADMIN));

        assertThat(userRepository.existsByRole(Role.ROLE_ADMIN)).isTrue();
    }

    @Test
    void existsByRole_shouldReturnFalse_whenNoUserHasThatRole() {

        userRepository.save(user("john@example.com", Role.ROLE_USER));

        assertThat(userRepository.existsByRole(Role.ROLE_ADMIN)).isFalse();
    }

    @Test
    void existsByEmailIgnoreCase_shouldReturnTrue_regardlessOfStoredOrQueriedCasing() {

        userRepository.save(user("John@Example.com"));

        assertThat(userRepository.existsByEmailIgnoreCase("john@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("JOHN@EXAMPLE.COM")).isTrue();
    }

    @Test
    void existsByEmailIgnoreCase_shouldReturnFalse_whenUserMissing() {

        assertThat(userRepository.existsByEmailIgnoreCase("ghost@example.com")).isFalse();
    }

    @Test
    void findByEmailIgnoreCase_shouldReturnUser_regardlessOfStoredOrQueriedCasing() {

        userRepository.save(user("John@Example.com"));

        Optional<User> found = userRepository.findByEmailIgnoreCase("JOHN@EXAMPLE.COM");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("John@Example.com");
    }

    @Test
    void findByEmailIgnoreCase_shouldReturnEmpty_whenMissing() {

        assertThat(userRepository.findByEmailIgnoreCase("ghost@example.com")).isEmpty();
    }
}
