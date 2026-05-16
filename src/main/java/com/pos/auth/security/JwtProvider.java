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

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(

                        new Date(

                                System.currentTimeMillis()
                                        + 604800000
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

        try {

            Jwts.parser()

                    .setSigningKey(
                            getSignKey()
                    )

                    .build()

                    .parseClaimsJws(
                            token
                    );

            return true;

        } catch (Exception ex) {

            return false;
        }
    }
}