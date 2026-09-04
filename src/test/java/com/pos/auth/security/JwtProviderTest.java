package com.pos.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {

        jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(
                jwtProvider,
                "jwtSecret",
                "test-secret-key-test-secret-key-test-secret-key"
        );

        ReflectionTestUtils.setField(
                jwtProvider,
                "jwtExpiration",
                3600000L
        );
    }

    @Test
    void generateToken_shouldEncodeSubjectExtractableViaEmail() {

        String token = jwtProvider.generateToken("john@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.extractEmail(token)).isEqualTo("john@example.com");
        assertThat(jwtProvider.extractUsername(token)).isEqualTo("john@example.com");
    }

    @Test
    void validateToken_shouldReturnTrue_forAccessToken() {

        String token = jwtProvider.generateToken("john@example.com");

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_forMalformedToken() {

        assertThat(jwtProvider.validateToken("not-a-valid-jwt")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forTokenSignedWithDifferentSecret() {

        JwtProvider otherProvider = new JwtProvider();

        ReflectionTestUtils.setField(
                otherProvider,
                "jwtSecret",
                "a-completely-different-secret-key-value-here"
        );

        ReflectionTestUtils.setField(
                otherProvider,
                "jwtExpiration",
                3600000L
        );

        String tokenFromOtherProvider = otherProvider.generateToken("john@example.com");

        assertThat(jwtProvider.validateToken(tokenFromOtherProvider)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forExpiredToken() {

        ReflectionTestUtils.setField(
                jwtProvider,
                "jwtExpiration",
                -1000L
        );

        String expiredToken = jwtProvider.generateToken("john@example.com");

        assertThat(jwtProvider.validateToken(expiredToken)).isFalse();
    }
}
