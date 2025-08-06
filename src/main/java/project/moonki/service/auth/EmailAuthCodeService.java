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

    // 인증번호 저장 (만료시간 포함)
    @Transactional
    public void saveCode(String email, String code, int minutes) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(minutes);
        codeRepository.deleteByEmail(email); // 이메일당 1건만 유지
        EmailAuthCode entity = new EmailAuthCode();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setExpiresAt(expiresAt);
        codeRepository.save(entity);
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

