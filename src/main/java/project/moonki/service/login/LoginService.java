package project.moonki.service.login;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.muser.LoginRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.MuserRepository;

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
        MUser user = muserRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요"));

        // 암호화된 비밀번호와 비교
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요");
        }
        return MUserMapper.toResponse(user);
    }
}
