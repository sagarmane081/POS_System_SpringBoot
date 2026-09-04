package com.pos.auth.service;

import com.pos.auth.entity.RefreshToken;
import com.pos.auth.entity.User;
import com.pos.auth.repository.RefreshTokenRepository;
import com.pos.common.exception.InvalidRefreshTokenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;

    private User user() {

        return User.builder().id(1L).email("john@example.com").build();
    }

    private void initService() {

        service = new RefreshTokenService(refreshTokenRepository);
    }

    @Test
    void issue_shouldPersistHashedTokenAndReturnRawToken() {

        initService();

        String rawToken = service.issue(user());

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void issue_shouldProduceDifferentTokens_onEachCall() {

        initService();

        String first = service.issue(user());
        String second = service.issue(user());

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rotate_shouldThrowInvalidRefreshTokenException_whenTokenBlank() {

        initService();

        assertThatThrownBy(() -> service.rotate(""))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        assertThatThrownBy(() -> service.rotate(null))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void rotate_shouldThrowInvalidRefreshTokenException_whenTokenNotFound() {

        initService();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void rotate_shouldThrowInvalidRefreshTokenException_whenTokenExpired() {

        initService();

        User user = user();

        RefreshToken expired = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotate_shouldRevokeOldTokenAndIssueNewOne_whenTokenValid() {

        initService();

        User user = user();

        RefreshToken valid = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(valid));

        RefreshTokenService.RotationResult result = service.rotate("valid-token");

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(valid.isRevoked()).isTrue();

        verify(refreshTokenRepository).save(valid);
        // one save() for revoking the old token, one for persisting the newly issued token
        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void rotate_shouldRevokeAllActiveTokensForUser_whenAlreadyRevokedTokenIsReused() {

        initService();

        User user = user();

        RefreshToken reused = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .user(user)
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        RefreshToken otherActiveToken = RefreshToken.builder()
                .id(2L)
                .tokenHash("other-hash")
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(reused));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user))
                .thenReturn(List.of(otherActiveToken));

        assertThatThrownBy(() -> service.rotate("reused-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        assertThat(otherActiveToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(otherActiveToken));
    }

    @Test
    void revoke_shouldMarkMatchingTokenAsRevoked() {

        initService();

        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .user(user())
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.revoke("some-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revoke_shouldBeNoOp_whenTokenBlankOrNotFound() {

        initService();

        service.revoke(null);
        service.revoke("");

        verifyNoInteractions(refreshTokenRepository);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revoke("unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }
}
