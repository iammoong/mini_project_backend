package project.moonki.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.moonki.dto.muser.LoginRequestDto;
import project.moonki.dto.muser.SignupRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.repository.MuserRepository;
import project.moonki.service.muser.MUserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final MUserService mUserService;
    private final MuserRepository muserRepository;

    /***
     * 회원가입
     *
     * @param req
     * @return
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@RequestBody SignupRequestDto req) {
        return ResponseEntity.ok(mUserService.signup(req));
    }

    /***
     * 로그인
     *
     * @param req
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@RequestBody LoginRequestDto req) {
        return ResponseEntity.ok(mUserService.login(req));
    }

    /***
     * 아이디 중복 체크
     *
     * @param userId
     * @return
     */
    @GetMapping("/exists/userId")
    public ResponseEntity<Boolean> checkUserId(@RequestParam String userId) {
        boolean exists = muserRepository.existsByUserId(userId);
        return ResponseEntity.ok(exists);
    }

    /***
     * 닉네임 중복 체크
     *
     * @param nickname
     * @return
     */
    @GetMapping("/exists/nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean exists = muserRepository.existsByNickname(nickname);
        return ResponseEntity.ok(exists);
    }
}
