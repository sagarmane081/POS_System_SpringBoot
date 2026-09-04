package com.pos.auth.service;

import com.pos.auth.entity.RefreshToken;
import com.pos.auth.entity.User;
import com.pos.auth.repository.RefreshTokenRepository;
import com.pos.common.exception.InvalidRefreshTokenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Refresh tokens are opaque, random, server-side-tracked strings (not JWTs),
 * so they can actually be revoked. Each successful refresh rotates the token
 * (the old one is single-use); presenting an already-rotated token is treated
 * as a possible theft and revokes every other active token for that user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final long EXPIRATION_SECONDS = 604_800L; // 7 days
    private static final String INVALID_MESSAGE = "Invalid or expired refresh token";

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(User user) {

        String rawToken = generateRawToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(EXPIRATION_SECONDS))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {

            throw new InvalidRefreshTokenException(INVALID_MESSAGE);
        }

        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException(INVALID_MESSAGE));

        if (existing.isRevoked()) {

            log.warn(
                    "Reuse of a revoked refresh token detected for user {}; revoking all active tokens",
                    existing.getUser().getEmail()
            );

            revokeAllActiveTokens(existing.getUser());

            throw new InvalidRefreshTokenException(INVALID_MESSAGE);
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new InvalidRefreshTokenException(INVALID_MESSAGE);
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUser());

        return new RotationResult(existing.getUser(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {

            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {

                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private void revokeAllActiveTokens(User user) {

        List<RefreshToken> active =
                refreshTokenRepository.findAllByUserAndRevokedFalse(user);

        active.forEach(token -> token.setRevoked(true));

        refreshTokenRepository.saveAll(active);
    }

    private String generateRawToken() {

        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    public record RotationResult(User user, String refreshToken) {
    }
}
