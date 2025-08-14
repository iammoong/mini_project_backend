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
import project.moonki.utils.LogUtil;
import project.moonki.utils.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private static final String KAKAO_USERID_PREFIX = "kakao_";
    private static final String FALLBACK_EMAIL_DOMAIN = "@none.local";
    private static final String DEFAULT_USERNAME = "카카오사용자";

    private final KakaoClient kakaoClient;
    private final MuserRepository users;
    private final JwtTokenProvider jwt;
    private final PasswordEncoder passwordEncoder;

    /***
     * 카카오 로그인/가입, 정보 동기화, 토큰 발급
     * @param code
     * @return
     */
    @Transactional
    public LoginResponseDto loginWithKakao(String code) {
        log.info("[KakaoService] 로그인 시작 - code={}", code);
        try {
            // 토큰 교환 & 사용자 조회
            var token = kakaoClient.exchangeToken(code);
            log.info("[KakaoService] 토큰 발급 성공");
            var me = kakaoClient.fetchUser(token.getAccessToken());
            log.info("[KakaoService] 사용자 정보 조회 성공 - id={}", me.getId());

            // 카카오 데이터 파싱
            Long kakaoId = me.getId();
            String kakaoIdStr = String.valueOf(kakaoId);
            String email = resolveEmail(me);
            String nickname = resolveNickname(me);
            String userId = KAKAO_USERID_PREFIX + kakaoId;

            // 가입 또는 조회 + 필드 동기화
            MUser user = upsertUser(kakaoIdStr, userId, email, nickname);

            // JWT 발급
            String jwtToken = jwt.generateToken(user.getUserId());

            // 응답 DTO
            UserResponseDto userDto = toUserResponse(user);
            log.info("[KakaoService] 로그인 성공 - userId={}, token 발급", user.getUserId());
            return LoginResponseDto.builder()
                    .user(userDto)
                    .token(jwtToken)
                    .build();

        } catch (Exception e) {
            LogUtil.error(log, KakaoService.class, e);
            throw e;
        }
    }

    // 이메일: 카카오 제공 값 없으면 대체 이메일 생성
    private String resolveEmail(Object me) {
        var account = getAccount(me);
        String emailFromKakao = (account != null) ? account.getEmail() : null;
        if (emailFromKakao != null && !emailFromKakao.isBlank()) {
            return emailFromKakao;
        }
        Long id = getId(me);
        return KAKAO_USERID_PREFIX + id + FALLBACK_EMAIL_DOMAIN;
    }

    // 닉네임: 제공 없으면 랜덤
    private String resolveNickname(Object me) {
        var account = getAccount(me);
        var profile = (account != null) ? account.getProfile() : null;
        String nickname = (profile != null) ? profile.getNickname() : null;
        return (nickname != null && !nickname.isBlank())
                ? nickname
                : PasswordUtil.generateRandomPassword(10);
    }

    // 신규 가입 또는 기존 사용자 조회 후 이메일/이름 동기화
    private MUser upsertUser(String kakaoIdStr, String userId, String email, String nickname) {
        MUser user = users.findByKakaoId(kakaoIdStr).orElseGet(() -> {
            String encoded = passwordEncoder.encode(PasswordUtil.generateRandomPassword(12));
            return users.save(MUser.builder()
                    .kakaoId(kakaoIdStr)
                    .userId(userId)
                    .username(Objects.requireNonNullElse(nickname, DEFAULT_USERNAME))
                    .nickname(nickname)
                    .email(email)
                    .password(encoded)
                    .createdAt(LocalDateTime.now())
                    .role(Role.USER)
                    .build());
        });

        boolean changed = false;
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }
        if (nickname != null && !nickname.equals(user.getUsername())) {
            user.setUsername(nickname);
            changed = true;
        }
        if (changed) {
            users.save(user);
        }
        return user;
    }

    private UserResponseDto toUserResponse(MUser user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .kakaoId(user.getKakaoId())
                .build();
    }


    private KakaoAccountDto getAccount(Object me) {
        try {
            var method = me.getClass().getMethod("getKakao_account");
            return (KakaoAccountDto) method.invoke(me);
        } catch (Exception ignore) {
            return null;
        }
    }

    private Long getId(Object me) {
        try {
            var method = me.getClass().getMethod("getId");
            return (Long) method.invoke(me);
        } catch (Exception e) {
            return null;
        }
    }
}
