package project.moonki.controller.login;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.kakao.KakaoUserDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.service.login.KakaoService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth/kakao")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final MuserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank())
            return ResponseEntity.badRequest().body("카카오 인가코드(code)가 필요합니다.");

        // 1. 액세스 토큰 교환
        String kakaoAccess = kakaoService.getAccessToken(code);

        // 2. 사용자 정보
        KakaoUserDto kakaoUser = kakaoService.getKakaoUserInfo(kakaoAccess);

        // 3. 우리 User upsert
        MUser user = userRepository.findByKakaoId(kakaoUser.getId())
                .orElseGet(() -> {
                    MUser created = MUser.builder()
                            .userId(kakaoUser.getEmail() != null ? kakaoUser.getEmail() : "kakao_" + kakaoUser.getId())
                            .username(kakaoUser.getNickname() != null ? kakaoUser.getNickname() : "카카오사용자")
                            .email(kakaoUser.getEmail() != null ? kakaoUser.getEmail() : ("kakao_" + kakaoUser.getId() + "@none.local"))
                            .kakaoId(kakaoUser.getId())
                            .build();
                    return userRepository.save(created);
                });

        // 4. JWT 발급 (기존 JwtTokenProvider 재사용)
        String jwt = jwtTokenProvider.generateToken(user.getUserId());

        Map<String, Object> res = new HashMap<>();
        res.put("token", jwt);
        res.put("userId", user.getUserId());
        res.put("username", user.getUsername());
        return ResponseEntity.ok(res);
    }
}
