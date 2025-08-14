package project.moonki.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import project.moonki.domain.user.entity.MUserImage;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.muser.ChangePasswordRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.dto.muser.UserUpdateRequestDto;
import project.moonki.repository.user.MUserImageRepository;
import project.moonki.service.muser.AccountService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final MUserImageRepository mUserImageRepository;

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
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequestDto req
    ) {
        accountService.changePassword(authentication, req);
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * 이미지 업로드
     * @param authentication
     * @param file
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponseDto> uploadMyProfileImage(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) throws Exception{
        UserResponseDto dto = accountService.uploadProfileImage(authentication, file);
        return ResponseEntity.ok(dto);
    }

    /**
     * 이미지 저장 경로 조회
     *
     * @param id
     * @return
     */
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws IOException {
        MUserImage img = mUserImageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        FileSystemResource resource = new FileSystemResource(img.getPath());
        if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
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
