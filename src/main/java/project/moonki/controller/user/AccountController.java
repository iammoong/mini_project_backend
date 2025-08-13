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

    /***
     * 사용자 정보 가져오기
     *
     * @param authentication
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(Authentication authentication) {
        return ResponseEntity.ok(accountService.getMe(authentication));
    }

    /***
     * 사용자 정보 변경
     *
     * @param authentication
     * @param request
     * @return
     */
    @PutMapping("/me")
    public ResponseEntity<LoginResponseDto> updateMe(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequestDto request
    ) {
        return ResponseEntity.ok(accountService.updateMe(authentication, request));
    }

    /***
     *  비밀번호 변경
     * @param authentication
     * @param req
     * @return
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword( Authentication authentication,
                                                @Valid @RequestBody ChangePasswordRequestDto req
    ) {
        accountService.changePassword(authentication, req);
        return ResponseEntity.noContent().build(); // 204
    }

    /***
     * 회원탈퇴: 인증 사용자 계정 삭제
     * @param authentication
     * @return
     */
    @DeleteMapping("/me/delete")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        accountService.deleteMe(authentication);
        return ResponseEntity.noContent().build(); // 204
    }
}
