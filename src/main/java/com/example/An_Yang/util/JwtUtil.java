package com.example.An_Yang.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String secretConfigured,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.issuer:anyang-app}") String issuer
    ) {
        this.key = buildKey(secretConfigured);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    private Key buildKey(String secretConfigured) {
        try {
            // 1) Base64로 보이면 디코드 시도
            byte[] raw = io.jsonwebtoken.io.Decoders.BASE64.decode(secretConfigured);
            return Keys.hmacShaKeyFor(raw); // 최소 256비트 확인 포함
        } catch (Exception ignore) {
            // 2) Base64 아니면 평문을 받아 SHA-256으로 256비트 키 생성
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(secretConfigured.getBytes(StandardCharsets.UTF_8));
                return new SecretKeySpec(digest, "HmacSHA256");
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid jwt.secret", e);
            }
        }
    }

    /** access 토큰 생성 - subject에 userId 저장 */
    public String generateAccessToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 토큰 유효성 검사 */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** subject(userId) */
    public String getSubject(String token) {
        return parse(token).getBody().getSubject();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
    }
}
