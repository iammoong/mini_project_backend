package project.moonki.service.login;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.muser.LoginRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.user.MuserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final MuserRepository muserRepository;
    private final PasswordEncoder passwordEncoder;

    /***
     * 로그인
     *
     * @param req
     * @return
     */
    @Transactional(readOnly = true)
    public UserResponseDto login(@RequestBody LoginRequestDto req) {
        try {
            MUser user = muserRepository.findByUserId(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요"));

            // 암호화된 비밀번호와 비교
            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요");
            }

            return MUserMapper.toResponse(user);

        } catch (IllegalArgumentException e) {
            // 사용자 입력 오류 처리
            throw e; // 그대로 다시 던져서 ControllerAdvice 등에서 처리
        } catch (Exception e) {
            // 예상치 못한 서버 오류 로깅
            log.error("로그인 처리 중 예외 발생", e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.");
        }
    }
}
