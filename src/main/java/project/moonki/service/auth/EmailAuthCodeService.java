package project.moonki.service.auth;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.domain.auth.EmailAuthCode;
import project.moonki.repository.auth.EmailAuthCodeRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthCodeService {

    private final EmailAuthCodeRepository codeRepository;

    private static final int MAX_REQUEST = 5;
    private static final int BLOCK_MINUTES = 5;

    /***
     * 인증번호 저장
     *
     * @param email : 이메일
     * @param code : 코드
     * @param minutes : 남은 시간
     * @return : String
     */
    @Transactional
    public String saveCodeWithLimit(String email, String code, int minutes) {
        try {
            LocalDateTime now = LocalDateTime.now();
            EmailAuthCode entity = codeRepository.findByEmail(email).orElse(null);

            if (entity != null) {
                // 차단 여부 확인
                if (entity.getBlockedUntil() != null && now.isBefore(entity.getBlockedUntil())) {
                    long remainSec = java.time.Duration.between(now, entity.getBlockedUntil()).toSeconds();
                    return "인증번호 요청 횟수를 초과하였습니다. " +
                            (remainSec > 60 ? (remainSec / 60) + "분" : remainSec + "초") +
                            " 뒤에 다시 요청하세요.";
                }

                // 5분 내 요청 횟수 제한
                if (entity.getRequestCount() == null || entity.getCreatedAt() == null
                        || entity.getCreatedAt().isBefore(now.minusMinutes(BLOCK_MINUTES))) {
                    entity.setRequestCount(1);
                    entity.setCreatedAt(now);
                    entity.setBlockedUntil(null);
                } else if (entity.getRequestCount() < MAX_REQUEST) {
                    entity.setRequestCount(entity.getRequestCount() + 1);
                } else {
                    entity.setBlockedUntil(now.plusMinutes(BLOCK_MINUTES));
                    codeRepository.save(entity);
                    return "인증번호 요청 횟수를 초과하였습니다. " + BLOCK_MINUTES + "분 뒤에 다시 요청하세요.";
                }

                entity.setCode(code);
                entity.setExpiresAt(now.plusMinutes(minutes));
                codeRepository.save(entity);

            } else {
                EmailAuthCode authCode = new EmailAuthCode();
                authCode.setEmail(email);
                authCode.setCode(code);
                authCode.setExpiresAt(now.plusMinutes(minutes));
                authCode.setCreatedAt(now);
                authCode.setRequestCount(1);
                codeRepository.save(authCode);
            }
            return null; // 정상 처리

        } catch (DataAccessException e) {
            log.error("이메일 인증코드 저장 중 데이터 접근 예외 - email={}", email, e);
            throw new RuntimeException("인증번호 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        } catch (Exception e) {
            log.error("이메일 인증코드 저장 중 예기치 못한 예외 - email={}", email, e);
            throw new RuntimeException("인증번호 처리 중 오류가 발생했습니다.");
        }
    }

    /***
     * 인증번호 검증
     *
     * @param email :이메일
     * @param code : 코드
     * @return : boolean
     */
    @Transactional
    public boolean checkCode(String email, String code) {
        try {
            Optional<EmailAuthCode> opt = codeRepository.findByEmail(email);
            if (opt.isPresent()) {
                EmailAuthCode authCode = opt.get();
                boolean valid = authCode.getCode().equals(code)
                        && authCode.getExpiresAt().isAfter(LocalDateTime.now());
                if (valid) {
                    codeRepository.deleteByEmail(email); // 1회용
                }
                return valid;
            }
            return false;

        } catch (DataAccessException e) {
            log.error("이메일 인증코드 검증 중 데이터 접근 예외 - email={}", email, e);
            throw new RuntimeException("인증번호 검증 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        } catch (Exception e) {
            log.error("이메일 인증코드 검증 중 예기치 못한 예외 - email={}", email, e);
            throw new RuntimeException("인증번호 검증 중 오류가 발생했습니다.");
        }
    }
}

