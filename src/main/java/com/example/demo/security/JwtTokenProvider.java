package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private Key getSigningKey() {
        logger.debug("Using JWT secret: {}", jwtSecret);
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        logger.debug("Generated token for user: {}", userDetails.getUsername());
        return token;
    }

    public String getUsernameFromToken(String token) {
        try {
            logger.debug("Extracting username from token...");
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String username = claims.getSubject();
            logger.debug("Username extracted: {}", username);
            return username;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            logger.debug("Validating token...");
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            logger.debug("Token validation successful");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Token validation failed: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 