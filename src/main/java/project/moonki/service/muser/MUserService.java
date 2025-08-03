package project.moonki.service.muser;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.dto.muser.LoginRequestDto;
import project.moonki.dto.muser.SignupRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.domain.user.entity.MUser;
import project.moonki.repository.MuserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MUserService {

    private final MuserRepository muserRepository;

    /***
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
                .password(req.getPassword())
                .nickname(req.getNickname())
                .createdAt(LocalDateTime.now())
                .build();

        muserRepository.save(user);

        // 엔티티 → DTO 변환
        return toResponse(user);
    }

    /***
     * 로그인
     *
     * @param req
     * @return
     */
    @Transactional(readOnly = true)
    public UserResponseDto login(LoginRequestDto req) {
        MUser user = muserRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요"));
        if (!user.getPassword().equals(req.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요");
        }
        return toResponse(user);
    }




    /***
     * 엔티티 -> DTO 변환 (공통화)
     *
     * @param user
     * @return
     */
    private UserResponseDto toResponse(MUser user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

}
