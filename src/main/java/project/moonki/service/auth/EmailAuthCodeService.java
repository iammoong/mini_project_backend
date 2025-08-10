package project.moonki.service.auth;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.moonki.domain.auth.EmailAuthCode;
import project.moonki.repository.auth.EmailAuthCodeRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailAuthCodeService {

    @Autowired
    private EmailAuthCodeRepository codeRepository;

    private static final int MAX_REQUEST = 5;
    private static final int BLOCK_MINUTES = 5;

    // 인증번호 저장
    @Transactional
    public String saveCodeWithLimit(String email, String code, int minutes) {
        LocalDateTime now = LocalDateTime.now();
        EmailAuthCode entity = codeRepository.findByEmail(email).orElse(null);

        if (entity != null) {
            // 이미 차단 중인지 확인
            if (entity.getBlockedUntil() != null && now.isBefore(entity.getBlockedUntil())) {
                long remainSec = java.time.Duration.between(now, entity.getBlockedUntil()).toSeconds();
                return "인증번호 요청 횟수를 초과하였습니다. " +
                        (remainSec > 60 ? (remainSec / 60) + "분" : remainSec + "초") +
                        " 뒤에 다시 요청하세요.";
            }

            // 5분 내 5회 초과 여부 판단
            if (entity.getRequestCount() == null || entity.getCreatedAt() == null
                    || entity.getCreatedAt().isBefore(now.minusMinutes(BLOCK_MINUTES))) {
                // 5분 지났으면 카운트 리셋
                entity.setRequestCount(1);
                entity.setCreatedAt(now);
                entity.setBlockedUntil(null);
            } else if (entity.getRequestCount() < MAX_REQUEST) {
                entity.setRequestCount(entity.getRequestCount() + 1);
            } else {
                // 차단 처리
                entity.setBlockedUntil(now.plusMinutes(BLOCK_MINUTES));
                codeRepository.save(entity);
                return "인증번호 요청 횟수를 초과하였습니다. " + BLOCK_MINUTES + "분 뒤에 다시 요청하세요.";
            }
            entity.setCode(code);
            entity.setExpiresAt(now.plusMinutes(minutes));
            codeRepository.save(entity);

        } else {
            // 신규 이메일(행 생성)
            EmailAuthCode newEntity = new EmailAuthCode();
            newEntity.setEmail(email);
            newEntity.setCode(code);
            newEntity.setExpiresAt(now.plusMinutes(minutes));
            newEntity.setCreatedAt(now);
            newEntity.setRequestCount(1);
            codeRepository.save(newEntity);
        }
        return null; // 정상 처리
    }

    // 인증번호 검증
    @Transactional
    public boolean checkCode(String email, String code) {
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
    }
}

