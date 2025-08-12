package project.moonki.service.muser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.dto.muser.ChangePasswordRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.dto.muser.UserUpdateRequestDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.service.login.LoginService;
import project.moonki.utils.LogUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional( readOnly = true)
public class AccountService {

    private final MuserRepository muserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto getMe(Authentication authentication) {
        try {
            MUser user = resolveUser(authentication);
            return MUserMapper.toResponse(user);
        } catch (Exception e) {
            LogUtil.error(log, AccountService.class, e);
            throw e; // 정책상 그대로 재던지기 (전역 예외 처리기에서 변환/응답)
        }
    }

    @Transactional
    public LoginResponseDto updateMe(Authentication authentication, UserUpdateRequestDto req) {
        try {
            MUser user = resolveUser(authentication);

            if (user.getKakaoId() != null) {
                throw new IllegalArgumentException("카카오 로그인 계정은 정보를 수정할 수 없습니다.");
            }

            // 아이디 변경
            boolean userIdChanged = false;
            if (req.getNewUserId() != null && !req.getNewUserId().isBlank()
                    && !req.getNewUserId().equals(user.getUserId())) {

                if (user.getKakaoId() != null) {
                    throw new IllegalArgumentException("카카오 로그인 계정은 아이디를 변경할 수 없습니다.");
                }
                if (muserRepository.existsByUserId(req.getNewUserId())) {
                    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
                }
                user.setUserId(req.getNewUserId());
                userIdChanged = true;
            }

            // 닉네임 변경
            if (req.getNickname() != null && !req.getNickname().equals(user.getNickname())) {
                if (muserRepository.existsByNickname(req.getNickname())) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
                user.setNickname(req.getNickname());
            }

            if (req.getUsername() != null) user.setUsername(req.getUsername());
            if (req.getEmail() != null)    user.setEmail(req.getEmail());
            if (req.getPhone() != null)    user.setPhone(req.getPhone());

            MUser saved = muserRepository.save(user);

            // 아이디 변경 시 새 토큰 발급
            String token = null;
            if (userIdChanged) {
                token = jwtTokenProvider.generateToken(saved.getUserId());
            }

            return new LoginResponseDto(MUserMapper.toResponse(saved), token);

        } catch (Exception e) {
            LogUtil.error(log, AccountService.class, e);
            throw e; // RuntimeException이면 @Transactional에 의해 롤백
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Authentication authentication, ChangePasswordRequestDto req) {
        try {
            // 인증 검증
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                throw new LoginService.UnauthorizedException("인증이 필요합니다.");
            }

            Object p = authentication.getPrincipal();
            if (!(p instanceof MUserDetailsDto principal)) {
                log.warn("[changePassword] Unexpected principal type: {}", p.getClass());
                throw new LoginService.UnauthorizedException("인증이 필요합니다.");
            }

            //  principal에 든 user는 영속 객체가 아닐 수 있어 id로 재조회(영속화)
            Long userPk = principal.getUser().getId();
            MUser user = muserRepository.findById(userPk)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            // 현재 비밀번호 검증
            if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
                log.warn("[changePassword] Current password mismatch: userId={}", user.getUserId());
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            // 동일 비밀번호 방지
            if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
                log.warn("[changePassword] New password equals old: userId={}", user.getUserId());
                throw new IllegalArgumentException("새 비밀번호가 기존과 동일합니다.");
            }

            // 비밀번호 변경 (더티체킹)
            String encoded = passwordEncoder.encode(req.newPassword());
            user.changePassword(encoded); // 엔티티 세터/메서드

            // 영속 상태이므로 별도 save() 없이 커밋 시 UPDATE 수행
            log.info("[changePassword] Password changed: userId={}", user.getUserId());

        } catch (LoginService.UnauthorizedException | IllegalArgumentException e) {
            log.warn("[changePassword] Client error: {}", e.getMessage());
            throw e; // 전역 예외 핸들러에서 401/400으로 매핑
        } catch (Exception e) {
            LogUtil.error(log, AccountService.class, e);
            throw new RuntimeException("비밀번호 변경 처리 중 오류가 발생했습니다.");
        }
    }

    private MUser resolveUser(Authentication authentication) {
        // resolveUser 내부는 그대로 두고, 상위 메서드에서 예외 로깅/처리 일원화
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("인증이 필요합니다.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof MUserDetailsDto)) {
            throw new IllegalStateException("인증 주체 유형이 올바르지 않습니다.");
        }
        return ((MUserDetailsDto) principal).getUser();
    }
}
