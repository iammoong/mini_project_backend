package project.moonki.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.auth.EmailAuthRequestDto;
import project.moonki.dto.auth.EmailAuthResponseDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.utils.MaskingUtil;
import project.moonki.utils.PasswordUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthFacade {

    private final EmailAuthCodeService codeService;
    private final MuserRepository muserRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    /***
     * 인증코드 발송
     *
     * @param request
     * @return
     */
    @Transactional
    public ResponseEntity<EmailAuthResponseDto> sendCode(EmailAuthRequestDto request) {
        try {
            String code = String.valueOf((int) (Math.random() * 900000) + 100000);
            int expiresTime = 5;

            String limitMsg = codeService.saveCodeWithLimit(request.getEmail(), code, expiresTime);
            if (limitMsg != null) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new EmailAuthResponseDto(false, limitMsg));
            }

            try {
                mailService.sendAuthCodeMail(request.getEmail(), code, "고객", expiresTime);
            } catch (Exception e) {
                log.error("인증코드 메일 발송 실패 - email={}", request.getEmail(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new EmailAuthResponseDto(false, "이메일 발송 실패"));
            }

            return ResponseEntity.ok(new EmailAuthResponseDto(true, "이메일로 인증번호가 발송되었습니다."));

        } catch (DataAccessException e) {
            log.error("인증코드 발송 처리 중 데이터 접근 예외 - email={}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailAuthResponseDto(false, "처리 중 오류가 발생했습니다."));
        } catch (Exception e) {
            log.error("인증코드 발송 처리 중 예기치 못한 예외 - email={}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailAuthResponseDto(false, "처리 중 오류가 발생했습니다."));
        }
    }

    /***
     * 이메일로 userId 조회
     *
     * @param email
     * @return
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> findUserId(String email) {
        try {
            Optional<MUser> userOpt = muserRepository.findByEmail(email);
            Map<String, String> result = new HashMap<>();
            result.put("userId", userOpt.map(MUser::getUserId).orElse(""));
            return ResponseEntity.ok(result);

        } catch (DataAccessException e) {
            log.error("userId 조회 중 데이터 접근 예외 - email={}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());
        } catch (Exception e) {
            log.error("userId 조회 중 예기치 못한 예외 - email={}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());

        }
    }

    /***
     * 코드 검증 후 userId 반환
     *
     * @param req
     * @return
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> checkCodeAndFindId(Map<String, String> req) {
        String email = req.get("email");
        String code = req.get("code");

        try {
            boolean valid = codeService.checkCode(email, code);

            Map<String, Object> result = new HashMap<>();
            result.put("success", valid);

            if (valid) {
                result.put("userId", muserRepository.findByEmail(email).map(MUser::getUserId).orElse(""));
            }
            return ResponseEntity.ok(result);

        } catch (DataAccessException e) {
            log.error("코드 검증/ID 조회 중 데이터 접근 예외 - email={}", email, e);
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        } catch (Exception e) {
            log.error("코드 검증/ID 조회 중 예기치 못한 예외 - email={}", email, e);
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /***
     * 아이디로 마스킹된 연락처 반환
     *
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> findUserForPw(String userId) {
        try {
            Map<String, String> result = new HashMap<>();
            return muserRepository.findByUserId(userId)
                    .map(user -> {
                        result.put("userId", user.getUserId());
                        result.put("emailMasked", MaskingUtil.maskEmail(user.getEmail()));
                        result.put("phoneMasked", MaskingUtil.maskPhone(user.getPhone()));
                        result.put("success", "true");
                        return ResponseEntity.ok(result);
                    })
                    .orElseGet(() -> {
                        result.put("success", "false");
                        return ResponseEntity.ok(result);
                    });

        } catch (DataAccessException e) {
            log.error("마스킹 정보 조회 중 데이터 접근 예외 - userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());
        } catch (Exception e) {
            log.error("마스킹 정보 조회 중 예기치 못한 예외 - userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>());
        }
    }

    /***
     * 임시 비밀번호 발급/전송
     *
     * @param req
     * @return
     */
    @Transactional
    public ResponseEntity<EmailAuthResponseDto> sendTempPassword(Map<String, String> req) {
        String userId = req.get("userId");
        String type = req.get("type"); // "email" or "phone"

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new EmailAuthResponseDto(false, "아이디를 입력해주세요."));
            }

            Optional<MUser> userOpt = muserRepository.findByUserId(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(new EmailAuthResponseDto(false, "존재하지 않는 아이디입니다."));
            }

            MUser user = userOpt.get();

            // 1) 임시 비밀번호 생성/저장
            String tempPassword = PasswordUtil.generateRandomPassword(10);
            user.setPassword(passwordEncoder.encode(tempPassword));
            muserRepository.save(user);

            // 2) 발송
            try {
                if ("email".equals(type)) {
                    mailService.sendTempPasswordMail(user.getEmail(), tempPassword, user.getUsername());
                } else if ("phone".equals(type)) {
                    // TODO: SMS 연동
                }
            } catch (Exception e) {
                log.error("임시 비밀번호 발송 실패 - userId={}", userId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new EmailAuthResponseDto(false, "임시비밀번호 발송 실패"));
            }

            return ResponseEntity.ok(new EmailAuthResponseDto(true, "임시비밀번호가 발송되었습니다."));

        } catch (DataAccessException e) {
            log.error("임시 비밀번호 처리 중 데이터 접근 예외 - userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailAuthResponseDto(false, "처리 중 오류가 발생했습니다."));
        } catch (Exception e) {
            log.error("임시 비밀번호 처리 중 예기치 못한 예외 - userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailAuthResponseDto(false, "처리 중 오류가 발생했습니다."));
        }
    }
}
