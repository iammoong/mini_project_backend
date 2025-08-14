package project.moonki.service.login;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.dto.login.LoginRequestDto;
import project.moonki.dto.muser.SignupRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.service.muser.MUserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final MUserService mUserService;
    private final MuserRepository muserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /***
     * 회원가입
     * @param req
     * @return
     */
    @Transactional
    public UserResponseDto signup(SignupRequestDto req) {
        return mUserService.signup(req);
    }

    /***
     * 로그인 + 토큰 발급
     *
     * @param req
     * @return
     */
    public LoginResponseDto login(LoginRequestDto req) {
        try {
            MUser user = muserRepository.findByUserId(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요"));

            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요");
            }

            String token = jwtTokenProvider.generateToken(user.getUserId());
            return new LoginResponseDto(MUserMapper.toResponse(user), token);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("로그인 처리 중 예외 발생", e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /***
     * 아이디 중복 체크
     *
     * @param userId
     * @return
     */
    public boolean existsUserId(String userId) {
        return muserRepository.existsByUserId(userId);
    }

    /***
     * 닉네임 중복 체크
     *
     * @param nickname
     * @return
     */
    public boolean existsNickname(String nickname) {
        return muserRepository.existsByNickname(nickname);
    }

    /***
     * 현재 사용자 정보
     *
     * @return
     */
    public UserResponseDto me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        MUserDetailsDto principal = (MUserDetailsDto) authentication.getPrincipal();
        return MUserMapper.toResponse(principal.getUser());
    }

    /**
     * 401 처리를 위한 도메인 예외
     *
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
}
