package project.moonki.service.muser;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.dto.muser.UserUpdateRequestDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;

@Service
@RequiredArgsConstructor
@Transactional( readOnly = true)
public class AccountService {

    private final MuserRepository muserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public UserResponseDto getMe(Authentication authentication) {
        MUser user = resolveUser(authentication);
        return MUserMapper.toResponse(user);
    }

    @Transactional
    public LoginResponseDto updateMe(Authentication authentication, UserUpdateRequestDto req) {
        MUser user = resolveUser(authentication);

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
    }

    private MUser resolveUser(Authentication authentication) {
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
