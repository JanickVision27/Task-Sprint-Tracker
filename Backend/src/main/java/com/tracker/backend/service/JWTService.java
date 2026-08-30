package com.tracker.backend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTService {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // ! Generates a JWT token for a given user email
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) // Who the token belongs to
                .issuedAt(new Date()) // When it was created
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // When it expires
                .signWith(getSigningKey()) // Sign it with our secret key
                .compact();
    }

    // ! Extracts the email from the token
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ! Checks if the token is still valid (not expired, signature matches)
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ! Helper: Converts our string secret into a proper encryption key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
