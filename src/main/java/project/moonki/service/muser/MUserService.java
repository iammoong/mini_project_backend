package project.moonki.service.muser;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.dto.muser.SignupRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.domain.user.entity.MUser;
import project.moonki.enums.Role;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.user.MuserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MUserService {

    private final MuserRepository muserRepository;
    private final PasswordEncoder passwordEncoder;


    /***
     * 회원가입
     *
     * @param req
     * @return
     */
    @Transactional
    public UserResponseDto signup(SignupRequestDto req) {
        // userId 중복 체크
        if(muserRepository.existsByUserId(req.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        MUser user = MUser.builder()
                .userId(req.getUserId())
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .email(req.getEmail())
                .phone(req.getPhone())
                .createdAt(LocalDateTime.now())
                .role(Role.USER)
                .build();

        muserRepository.save(user);

        // 엔티티 → DTO 변환
        return MUserMapper.toResponse(user);
    }



}
