package project.moonki.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * A utility class for handling JSON Web Tokens (JWT) operations such as creation, validation,
 * and extracting user information.
 *
 * This class is a Spring component and provides methods to:
 * - Create a JWT token with a specified expiration time.
 * - Validate existing JWT tokens for correctness.
 * - Extract user-specific details (userId) from a JWT token.
 */
@Component
public class JwtTokenProvider {
    private final String SECRET_KEY = "zQclYAzXpC5mAuOOx/SeTQnFdzt9N1RDe7T4uzjQfhg="; // 반드시 환경변수 처리
    private final long EXPIRATION = 1000L * 60 * 60; // 1시간

    // 토큰 생성
    public String generateToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 userId 추출
    public String getUserId(String token) {
        Claims claims = Jwts.parser().setSigningKey(SECRET_KEY)
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}

