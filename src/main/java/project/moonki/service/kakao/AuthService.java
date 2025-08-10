package project.moonki.service.kakao;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.components.kakao.KakaoClient;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.kakao.KakaoAccountDto;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.enums.Role;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.utils.PasswordUtil;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {private final KakaoClient kakaoClient;
    private final MuserRepository users;
    private final JwtTokenProvider jwt;
    private final PasswordEncoder passwordEncoder;

    /**
     * 카카오톡 로그인 로직
     *
     * @param code
     * @return
     */
    @Transactional
    public LoginResponseDto loginWithKakao(String code) {
        try {
            log.info("[AuthService] 카카오 로그인 시작 - code={}", code);

            // 인가코드로 카카오 토큰 교환
            var token = kakaoClient.exchangeToken(code);
            log.info("[AuthService] 토큰 발급 성공: {}", token);

            // 카카오 사용자 정보 조회
            var me = kakaoClient.fetchUser(token.getAccessToken());
            log.info("[AuthService] 사용자 정보 조회 성공: id={}, email={}, nickname={}",
                    me.getId(),
                    me.getKakao_account() != null ? me.getKakao_account().getEmail() : null,
                    (me.getKakao_account()!=null && me.getKakao_account().getProfile()!=null)
                            ? me.getKakao_account().getProfile().getNickname() : null);

            KakaoAccountDto acc = me.getKakao_account();
            Long kakaoId = me.getId();
            String emailFromKakao = (acc != null) ? acc.getEmail() : null;
            // DB 제약(email NOT NULL) 충족을 위해 대체 이메일 생성
            String email = (emailFromKakao != null && !emailFromKakao.isBlank())
                    ? emailFromKakao
                    : ("kakao_" + kakaoId + "@none.local");
            String nickname = (me.getKakao_account()!=null && me.getKakao_account().getProfile()!=null)
                    ? me.getKakao_account().getProfile().getNickname() : PasswordUtil.generateRandomPassword(10);

            // userId 결정
            String userId = (email != null && !email.isBlank()) ? email : ("kakao_" + kakaoId);
            String kakaoIdStr = String.valueOf(kakaoId);

            //  최초 가입 시에만 랜덤 비밀번호 생성 후 BCrypt로 암호화하여 저장
            MUser user = users.findByKakaoId(kakaoIdStr).orElseGet(() -> {
                String rawRandom = PasswordUtil.generateRandomPassword(12);
                //String nickname = PasswordUtil.generateRandomPassword(10);
                String encoded = passwordEncoder.encode(rawRandom);

                return users.save(MUser.builder()
                        .kakaoId(kakaoIdStr)
                        .userId(userId)
                        .username(nickname != null ? nickname : "카카오사용자")
                        .nickname(nickname)
                        .email(email)
                        .password(encoded)
                        .createdAt(LocalDateTime.now())
                        .role(Role.USER)
                        .build());
            });

            // 변경사항 동기화
            if (email != null && (user.getEmail() == null || !email.equals(user.getEmail()))) {
                user.setEmail(email);
            }
            if (nickname != null && (user.getUsername() == null || !nickname.equals(user.getUsername()))) {
                user.setUsername(nickname);
            }

            // JWT 발급
            String jwtToken = jwt.generateToken(user.getUserId());

            // UserResponseDto 구성
            UserResponseDto userDto = UserResponseDto.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .kakaoId(user.getKakaoId())
                    .build();

            // 최종 응답
            log.info("[AuthService] 로그인 성공 - userId={}, token발급", user.getUserId());
            return LoginResponseDto.builder()
                    .user(userDto)
                    .token(jwtToken)
                    .build();

        } catch (Exception e) {
            log.error("[AuthService] 카카오 로그인 실패", e);
            throw e;
        }
    }
}
