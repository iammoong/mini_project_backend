package project.moonki.service.kakao;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.components.kakao.KakaoClient;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.enums.Role;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AuthService {private final KakaoClient kakaoClient;
    private final MuserRepository users;
    private final JwtTokenProvider jwt; // 주신 클래스 그대로 사용(Subject= userId)

    @Transactional
    public LoginResponseDto loginWithKakao(String code) {
        // 1) 인가코드로 카카오 토큰 교환
        var token = kakaoClient.exchangeToken(code);
        // 2) 카카오 사용자 정보 조회
        var me = kakaoClient.fetchUser(token.getAccessToken());

        Long kakaoId = me.getId();
        String email = me.getKakao_account() != null ? me.getKakao_account().getEmail() : null;
        String nickname = (me.getKakao_account()!=null && me.getKakao_account().getProfile()!=null)
                ? me.getKakao_account().getProfile().getNickname() : null;

        // 3) 우리 쪽 userId 결정(이메일 있으면 이메일, 없으면 kakao_{id})
        String userId = (email != null && !email.isBlank()) ? email : ("kakao_" + kakaoId);

        // 현재 스키마가 kakaoId=String 이므로 String 변환
        String kakaoIdStr = String.valueOf(kakaoId);

        // 4) Upsert
        MUser user = users.findByKakaoId(kakaoIdStr).orElseGet(() ->
                users.save(MUser.builder()
                        .kakaoId(kakaoIdStr) // 엔티티가 String이면 String 유지
                        .userId(userId)
                        .username(nickname != null ? nickname : "카카오사용자")
                        .email(email)
                        .role(Role.USER)
                        .build())
        );
        // 변경사항 동기화(트랜잭션 내 더티체킹)
        if (email != null && (user.getEmail() == null || !email.equals(user.getEmail()))) {
            user.setEmail(email);
        }
        if (nickname != null && (user.getUsername() == null || !nickname.equals(user.getUsername()))) {
            user.setUsername(nickname);
        }

        // 5) JWT 발급(Subject = userId)
        String jwtToken = jwt.generateToken(user.getUserId());

        // 6) UserResponseDto 구성
        UserResponseDto userDto = UserResponseDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .kakaoId(user.getKakaoId())
                .build();

        // 7) 최종 응답
        return LoginResponseDto.builder()
                .user(userDto)    // <-- 객체 자체를 넣어야 함
                .token(jwtToken)
                .build();
    }
}
