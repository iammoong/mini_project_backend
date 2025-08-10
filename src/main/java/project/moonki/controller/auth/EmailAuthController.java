package project.moonki.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.moonki.dto.auth.EmailAuthRequestDto;
import project.moonki.dto.auth.EmailAuthResponseDto;
import project.moonki.service.auth.EmailAuthFacade;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/mail")
public class EmailAuthController {

    private final EmailAuthFacade emailAuthFacade;

    @PostMapping("/sendCode")
    public ResponseEntity<EmailAuthResponseDto> sendCode(@RequestBody EmailAuthRequestDto request) {
        return emailAuthFacade.sendCode(request);
    }

    @GetMapping("/findUserid")
    public ResponseEntity<Map<String, String>> findUserId(@RequestParam String email) {
        return emailAuthFacade.findUserId(email);
    }

    @PostMapping("/checkCodeAndFindId")
    public ResponseEntity<Map<String, Object>> checkCodeAndFindId(@RequestBody Map<String, String> req) {
        return emailAuthFacade.checkCodeAndFindId(req);
    }

    @GetMapping("/findUser")
    public ResponseEntity<Map<String, String>> findUserForPw(@RequestParam String userId) {
        return emailAuthFacade.findUserForPw(userId);
    }

    @PostMapping("/sendTempPw")
    public ResponseEntity<EmailAuthResponseDto> sendTempPassword(@RequestBody Map<String, String> req) {
        return emailAuthFacade.sendTempPassword(req);
    }
}
