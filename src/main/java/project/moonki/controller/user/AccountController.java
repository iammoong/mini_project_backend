package project.moonki.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.muser.ChangePasswordRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.dto.muser.UserUpdateRequestDto;
import project.moonki.service.muser.AccountService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // GET /auth/me
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(Authentication authentication) {
        return ResponseEntity.ok(accountService.getMe(authentication));
    }

    @PutMapping("/me")
    public ResponseEntity<LoginResponseDto> updateMe(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequestDto request
    ) {
        return ResponseEntity.ok(accountService.updateMe(authentication, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword( Authentication authentication,
                                                @Valid @RequestBody ChangePasswordRequestDto req
    ) {
        accountService.changePassword(authentication, req);
        return ResponseEntity.noContent().build(); // 204
    }
}
