package project.moonki.controller.login;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.LoginResponse;
import project.moonki.dto.login.MUserDetails;
import project.moonki.dto.muser.LoginRequestDto;
import project.moonki.dto.muser.SignupRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.service.login.LoginService;
import project.moonki.service.muser.MUserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final MUserService mUserService;
    private final MuserRepository muserRepository;
    private final LoginService loginService;
    private final JwtTokenProvider jwtTokenProvider;

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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequestDto req) {
        UserResponseDto user = loginService.login(req); // 비밀번호 등 검증
        String token = jwtTokenProvider.generateToken(user.getUserId());
        LoginResponse loginResponse = new LoginResponse(user, token);
        return ResponseEntity.ok(loginResponse);
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

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        MUserDetails principal = (MUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(MUserMapper.toResponse(principal.getUser()));
    }
}
