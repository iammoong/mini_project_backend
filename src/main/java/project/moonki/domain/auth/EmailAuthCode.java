package project.moonki.domain.auth;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_auth_code")
@Getter
@Setter
public class EmailAuthCode {

    @Id @GeneratedValue
    private Long id;
    private String email;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt = LocalDateTime.now();

}
