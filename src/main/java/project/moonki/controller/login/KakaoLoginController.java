package project.moonki.controller.login;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.service.kakao.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoLoginController {

    private final AuthService authService; // ✅ 새 AuthService만 주입

    // 요청 바디용 DTO (record)
    public record CodeRequest(@NotBlank String code) {}

    /**
     * 카카오 인가코드(code)를 받아
     * - 카카오 토큰 교환
     * - 카카오 사용자 조회
     * - 우리 사용자 upsert
     * - 우리 JWT 발급
     * 을 수행하고 LoginResponseDto(user, token)을 반환합니다.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Validated @RequestBody CodeRequest req) {
        LoginResponseDto result = authService.loginWithKakao(req.code());
        return ResponseEntity.ok(result);
    }
}
