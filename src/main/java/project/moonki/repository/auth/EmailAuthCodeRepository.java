package project.moonki.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import project.moonki.domain.auth.EmailAuthCode;

import java.util.Optional;

public interface EmailAuthCodeRepository extends JpaRepository<EmailAuthCode, Long> {
    Optional<EmailAuthCode> findByEmail(String email);
    void deleteByEmail(String email);
}
