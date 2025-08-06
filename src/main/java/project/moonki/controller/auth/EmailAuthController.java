package project.moonki.controller.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.auth.EmailAuthRequestDto;
import project.moonki.dto.auth.EmailAuthResponseDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.service.auth.EmailAuthCodeService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/mail")
public class EmailAuthController {
    private final JavaMailSender mailSender;
    private final EmailAuthCodeService codeService;
    private final MuserRepository muserRepository;

    // 인증번호 발송 (DB 저장)
    @PostMapping("/sendCode")
    public ResponseEntity<EmailAuthResponseDto> sendCode(@RequestBody EmailAuthRequestDto request) {
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);

        // 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getEmail());
        message.setSubject("[서비스명] 인증번호 안내");
        message.setText("인증번호는 " + code + " 입니다. 5분 이내에 입력해 주세요.");
        mailSender.send(message);

        // MySQL 테이블에 인증번호 저장
        codeService.saveCode(request.getEmail(), code, 5); // 5분 유효

        EmailAuthResponseDto res = new EmailAuthResponseDto();
        res.setSuccess(true);
        res.setMessage("이메일로 인증번호가 발송되었습니다.");
        return ResponseEntity.ok(res);
    }

    // 인증번호 확인
    @PostMapping("/checkCode")
    public ResponseEntity<EmailAuthResponseDto> checkCode(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String code = req.get("code");

        boolean valid = codeService.checkCode(email, code);

        EmailAuthResponseDto res = new EmailAuthResponseDto();
        res.setSuccess(valid);
        res.setMessage(valid ? "인증 성공" : "인증 실패");
        return ResponseEntity.ok(res);
    }

    // 이메일로 아이디 찾기
    @GetMapping("/findUserid")
    public ResponseEntity<Map<String, String>> findUserId(@RequestParam String email) {
        Optional<MUser> userOpt = muserRepository.findByEmail(email);
        Map<String, String> result = new HashMap<>();
        result.put("userId", userOpt.map(MUser::getUserId).orElse(""));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/checkCodeAndFindId")
    public ResponseEntity<?> checkCodeAndFindId(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String code = req.get("code");
        boolean valid = codeService.checkCode(email, code);

        Map<String, Object> result = new HashMap<>();
        result.put("success", valid);

        if (valid) {
            Optional<MUser> userOpt = muserRepository.findByEmail(email);
            result.put("userId", userOpt.map(MUser::getUserId).orElse(""));
        }

        return ResponseEntity.ok(result);
    }

}
