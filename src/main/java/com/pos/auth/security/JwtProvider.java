package com.pos.auth.security;

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

    public String generateToken(String email) {

        Key key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes()
        );

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )
                .signWith(
                        key,
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public String extractEmail(String token) {

        return Jwts.parser()
                .setSigningKey(
                        Keys.hmacShaKeyFor(
                                jwtSecret.getBytes()
                        )
                )
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {

        try {

            Jwts.parser()
                    .setSigningKey(
                            Keys.hmacShaKeyFor(
                                    jwtSecret.getBytes()
                            )
                    )
                    .build()
                    .parseClaimsJws(token);

            return true;

        } catch (Exception ex) {

            return false;
        }
    }
}