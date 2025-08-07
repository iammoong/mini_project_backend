package project.moonki.controller.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.auth.EmailAuthRequestDto;
import project.moonki.dto.auth.EmailAuthResponseDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.service.auth.EmailAuthCodeService;
import project.moonki.service.auth.MailService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/mail")
public class EmailAuthController {

    private final EmailAuthCodeService codeService;
    private final MuserRepository muserRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    /***
     * 인증번호 발송 (DB 저장)
     *
     * @param request
     * @return
     */
    @PostMapping("/sendCode")
    public ResponseEntity<EmailAuthResponseDto> sendCode(@RequestBody EmailAuthRequestDto request) {
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        int expiresTime = 5;

        // 서비스 호출 (결과 메시지 null이면 정상, 아니면 제한 메시지)
        String limitMsg = codeService.saveCodeWithLimit(request.getEmail(), code, expiresTime);

        if (limitMsg != null) {
            EmailAuthResponseDto res = new EmailAuthResponseDto();
            res.setSuccess(false);
            res.setMessage(limitMsg);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(res);
        }

        try {
            mailService.sendAuthCodeMail(request.getEmail(), code, "고객", expiresTime);
        } catch (Exception e) {
            EmailAuthResponseDto res = new EmailAuthResponseDto();
            res.setSuccess(false);
            res.setMessage("이메일 발송 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }

        EmailAuthResponseDto res = new EmailAuthResponseDto();
        res.setSuccess(true);
        res.setMessage("이메일로 인증번호가 발송되었습니다.");
        return ResponseEntity.ok(res);
    }

    /***
     *  이메일 아이디 찾기
     *
     * @param email
     * @return
     */
    @GetMapping("/findUserid")
    public ResponseEntity<Map<String, String>> findUserId(@RequestParam String email) {
        Optional<MUser> userOpt = muserRepository.findByEmail(email);
        Map<String, String> result = new HashMap<>();
        result.put("userId", userOpt.map(MUser::getUserId).orElse(""));
        return ResponseEntity.ok(result);
    }

    /***
     * 인증번호 확인 후 아이디 찾기
     *
     * @param req
     * @return
     */
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

    // 아이디로 마스킹된 이메일/휴대폰 정보 반환 (비밀번호 찾기 1단계)
    @GetMapping("/findUser")
    public ResponseEntity<Map<String, String>> findUserForPw(@RequestParam String userId) {
        Optional<MUser> userOpt = muserRepository.findByUserId(userId);
        Map<String, String> result = new HashMap<>();
        if (userOpt.isPresent()) {
            MUser user = userOpt.get();
            result.put("userId", user.getUserId());
            result.put("emailMasked", maskEmail(user.getEmail()));
            result.put("phoneMasked", maskPhone(user.getPhone()));
            // 실제 이메일, 폰은 반환 X (마스킹만)
            result.put("success", "true");
        } else {
            result.put("success", "false");
        }
        return ResponseEntity.ok(result);
    }

    // 임시비밀번호 발급 및 메일 전송 (비밀번호 찾기 2단계)
    @PostMapping("/sendTempPw")
    public ResponseEntity<EmailAuthResponseDto> sendTempPassword(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String type = req.get("type"); // "email" or "phone"
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new EmailAuthResponseDto(false, "아이디를 입력해주세요."));
        }

        Optional<MUser> userOpt = muserRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(new EmailAuthResponseDto(false, "존재하지 않는 아이디입니다."));
        }
        MUser user = userOpt.get();

        // 1. 임시비밀번호 생성
        String tempPassword = EmailAuthCodeService.generateRandomPassword(10);
        // 2. 비밀번호 암호화 저장
        user.setPassword(passwordEncoder.encode(tempPassword));
        muserRepository.save(user);

        // 3. 메일/문자 전송
        try {
            if ("email".equals(type)) {
                mailService.sendTempPasswordMail(user.getEmail(), tempPassword, user.getUsername());
            } else if ("phone".equals(type)) {
                // 실제 문자발송 연동 필요 (여기선 생략)
                // smsService.sendTempPasswordSms(user.getPhone(), tempPassword);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailAuthResponseDto(false, "임시비밀번호 발송 실패: " + e.getMessage()));
        }

        return ResponseEntity.ok(new EmailAuthResponseDto(true, "임시비밀번호가 발송되었습니다."));
    }

    // 이메일 마스킹 유틸 (ex. te**@te**.com)
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@");
        String id = parts[0].toLowerCase();
        String domainFull = parts[1].toLowerCase();

        // 도메인 . 앞부분만 마스킹 (예: test.com -> test, com)
        String[] domainParts = domainFull.split("\\.", 2);
        String domain = domainParts[0];
        String tld = domainParts.length > 1 ? "." + domainParts[1] : "";

        // 마스킹 적용 함수
        java.util.Random random = new java.util.Random();
        String maskedId = maskWithRandomStars(id, random, 2);
        String maskedDomain = maskWithRandomStars(domain, random, 2);

        return (maskedId + "@" + maskedDomain + tld).toLowerCase();
    }

    // 글자수에 맞게 랜덤한 위치에 n개 * 표시 (단, 첫/마지막 글자는 되도록 유지)
    private String maskWithRandomStars(String src, java.util.Random random, int starCount) {
        if (src.length() <= 2) return src.charAt(0) + "*"; // ex: ab -> a*
        char[] arr = src.toCharArray();
        java.util.Set<Integer> maskedIdx = new java.util.HashSet<>();
        int tries = 0;
        while (maskedIdx.size() < Math.min(starCount, src.length()-1) && tries < 10) {
            int idx = 1 + random.nextInt(src.length()-2); // 1~len-2만 마스킹
            if (idx > 0 && idx < src.length()-1) maskedIdx.add(idx);
            tries++;
        }
        for (int idx : maskedIdx) arr[idx] = '*';
        return new String(arr);
    }

    // 휴대폰 마스킹 유틸 (ex. 010-12**-56**)
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "";
        return phone.replaceAll("(\\d{3})-?(\\d{2,4})-?(\\d{2,4})", "$1-**$2-**$3");
    }
}
