package project.moonki.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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
public class JwtTokenProvider {private final SecretKey key;        // HS256용 키
    private final long expirationMillis;

    // 설정/환경변수에서 주입되며, 값 미지정 시 기존 하드코딩 값으로 fallback
    public JwtTokenProvider(
            @Value("${jwt.secret:zQclYAzXpC5mAuOOx/SeTQnFdzt9N1RDe7T4uzjQfhg=}") String base64Secret,
            @Value("${jwt.expiration-ms:1800000}") long expirationMillis // 기본 30분
    ) {
        // Base64 디코딩된 바이트로 HMAC 키 생성 (256비트 이상 권장)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMillis = expirationMillis;
    }

    // 토큰 생성
    public String generateToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    // 토큰 검증 (서명/만료 모두 검증)
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 userId(subject) 추출
    public String getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}

