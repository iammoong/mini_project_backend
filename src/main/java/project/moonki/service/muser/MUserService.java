package project.moonki.service.muser;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

@Slf4j
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
        try {
            // userId 중복 체크
            if (muserRepository.existsByUserId(req.getUserId())) {
                throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
            }
            // 닉네임 중복 체크
            if (muserRepository.existsByNickname(req.getNickname())) {
                throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
            }
            // 이메일 중복 체크
            if (muserRepository.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("이미 등록된 이메일 아이디 입니다.");
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

            return MUserMapper.toResponse(user);

        } catch (IllegalArgumentException e) {
            // 사용자 입력(중복 등) 오류는 그대로 전달
            throw e;

        } catch (DataIntegrityViolationException e) {
            // DB 유니크 제약 등 무결성 위반
            log.error("회원가입 무결성 위반 - userId={}, nickname={}, email={}",
                    req.getUserId(), req.getNickname(), req.getEmail(), e);
            throw new RuntimeException("이미 사용 중인 정보가 있습니다. 입력값을 확인해 주세요.");

        } catch (Exception e) {
            // 예기치 못한 서버 오류
            log.error("회원가입 처리 중 예외 - userId={}, nickname={}, email={}",
                    req.getUserId(), req.getNickname(), req.getEmail(), e);
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.");
        }
    }



}
