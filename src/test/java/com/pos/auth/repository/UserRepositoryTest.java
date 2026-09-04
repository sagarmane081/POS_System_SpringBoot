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

        return User.builder()
                .name("John Doe")
                .email(email)
                .password("encoded-password")
                .role(Role.ROLE_USER)
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
}
