package project.moonki.controller.login;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.login.LoginRequestDto;
import project.moonki.dto.muser.ChangePasswordRequestDto;
import project.moonki.dto.muser.SignupRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.service.login.LoginService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/signup")
    public UserResponseDto signup(@RequestBody SignupRequestDto req) {
        return loginService.signup(req);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginRequestDto req) {
        return loginService.login(req);
    }

    @GetMapping("/exists/userId")
    public boolean checkUserId(@RequestParam String userId) {
        return loginService.existsUserId(userId);
    }

    @GetMapping("/exists/nickname")
    public boolean checkNickname(@RequestParam String nickname) {
        return loginService.existsNickname(nickname);
    }

    @GetMapping("/me")
    public UserResponseDto getMe() {
        return loginService.me();
    }


}

