package project.moonki.controller.login;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.KakaoUserDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.service.login.KakaoService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth/kakao")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final MuserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 1. 프론트에서 code 전달받아 카카오 로그인 처리
    @GetMapping("/callback")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> req) {
        String code = req.get("code");
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body("카카오 인가코드(code)가 필요합니다.");
        }

        try {
            // 1. 인가코드로 access token 발급
            String accessToken = kakaoService.getAccessToken(code);
            // 2. 사용자 정보 조회
            KakaoUserDto kakaoUser = kakaoService.getKakaoUserInfo(accessToken);

            // 1. 카카오 id/email로 기존 회원 있는지 조회
            Optional<MUser> userOpt = userRepository.findByKakaoId(kakaoUser.getId());
            MUser user;
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // 2. 없으면 회원 가입 (이메일이 PK라면 findByEmail)
                user = new MUser();
                user.setKakaoId(kakaoUser.getId()); // DB에 필드 추가 필요
                user.setUserId("kakao_" + kakaoUser.getId());
                user.setEmail(kakaoUser.getEmail());
                user.setUsername(kakaoUser.getNickname());
                user.setPassword(""); // 카카오회원은 별도의 패스워드 사용X
                // 필요하다면 기타 기본 값 세팅
                userRepository.save(user);
            }

            // 3. JWT 발급
            String jwtToken = jwtTokenProvider.generateToken(user.getUserId());

            // 4. 프론트에 토큰 전달
            Map<String, Object> res = new HashMap<>();
            res.put("token", jwtToken);
            res.put("userId", user.getUserId());
            res.put("username", user.getUsername());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("카카오 로그인 처리 실패: " + e.getMessage());
        }
    }
}
