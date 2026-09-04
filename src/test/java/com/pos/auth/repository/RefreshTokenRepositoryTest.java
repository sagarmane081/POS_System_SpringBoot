package com.pos.auth.repository;

import com.pos.auth.entity.RefreshToken;
import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {

        return userRepository.save(
                User.builder()
                        .name("John Doe")
                        .email(email)
                        .password("encoded-password")
                        .role(Role.ROLE_USER)
                        .build()
        );
    }

    private RefreshToken token(User user, String hash, boolean revoked) {

        return RefreshToken.builder()
                .tokenHash(hash)
                .user(user)
                .revoked(revoked)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findByTokenHash_shouldReturnToken_whenExists() {

        User user = persistUser("john@example.com");
        refreshTokenRepository.save(token(user, "hash-1", false));

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-1");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByTokenHash_shouldReturnEmpty_whenMissing() {

        assertThat(refreshTokenRepository.findByTokenHash("missing-hash")).isEmpty();
    }

    @Test
    void findAllByUserAndRevokedFalse_shouldReturnOnlyActiveTokensForThatUser() {

        User john = persistUser("john@example.com");
        User jane = persistUser("jane@example.com");

        refreshTokenRepository.save(token(john, "john-active-1", false));
        refreshTokenRepository.save(token(john, "john-active-2", false));
        refreshTokenRepository.save(token(john, "john-revoked", true));
        refreshTokenRepository.save(token(jane, "jane-active", false));

        List<RefreshToken> johnsActiveTokens =
                refreshTokenRepository.findAllByUserAndRevokedFalse(john);

        assertThat(johnsActiveTokens)
                .extracting(RefreshToken::getTokenHash)
                .containsExactlyInAnyOrder("john-active-1", "john-active-2");
    }

    @Test
    void findAllByUserAndRevokedFalse_shouldReturnEmpty_whenNoneActive() {

        User user = persistUser("john@example.com");
        refreshTokenRepository.save(token(user, "revoked-only", true));

        assertThat(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).isEmpty();
    }

    @Test
    void save_shouldEnforceUniqueTokenHash() {

        User user = persistUser("john@example.com");
        refreshTokenRepository.saveAndFlush(token(user, "duplicate-hash", false));

        assertThatThrownBy(() ->
                refreshTokenRepository.saveAndFlush(token(user, "duplicate-hash", false))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
