package com.pos.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final long REFRESH_EXPIRATION_MS = 604800000;

    private Key getSignKey() {

        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes()
        );
    }


    public String generateToken(
            String email
    ) {

        return Jwts.builder()

                .setSubject(email)

                .claim(TYPE_CLAIM, TYPE_ACCESS)

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(

                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )

                .signWith(
                        getSignKey(),
                        SignatureAlgorithm.HS256
                )

                .compact();
    }


    public String generateRefreshToken(
            String email
    ) {

        return Jwts.builder()

                .setSubject(email)

                .claim(TYPE_CLAIM, TYPE_REFRESH)

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(

                        new Date(

                                System.currentTimeMillis()
                                        + REFRESH_EXPIRATION_MS
                        )
                )

                .signWith(
                        getSignKey(),
                        SignatureAlgorithm.HS256
                )

                .compact();
    }


    public String extractEmail(
            String token
    ) {

        return Jwts.parser()

                .setSigningKey(
                        getSignKey()
                )

                .build()

                .parseClaimsJws(
                        token
                )

                .getBody()

                .getSubject();
    }


    public String extractUsername(
            String token
    ) {

        return extractEmail(
                token
        );
    }


    public boolean validateToken(
            String token
    ) {

        return isTokenOfType(token, TYPE_ACCESS);
    }


    public boolean validateRefreshToken(
            String token
    ) {

        return isTokenOfType(token, TYPE_REFRESH);
    }


    private boolean isTokenOfType(
            String token,
            String expectedType
    ) {

        try {

            Claims claims =
                    Jwts.parser()

                            .setSigningKey(
                                    getSignKey()
                            )

                            .build()

                            .parseClaimsJws(
                                    token
                            )

                            .getBody();

            return expectedType.equals(
                    claims.get(TYPE_CLAIM, String.class)
            );

        } catch (Exception ex) {

            return false;
        }
    }
}