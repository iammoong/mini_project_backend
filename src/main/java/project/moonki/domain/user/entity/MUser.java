package project.moonki.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import project.moonki.enums.Role;

@Entity
@Table(name = "m_user")
@Getter @Setter @AllArgsConstructor
@Builder
@NoArgsConstructor
public class MUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", length = 50)
    private String kakaoId;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId; // 로그인용 아이디

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    public void changePassword(String encodedPw) {
        this.password = encodedPw;
    }

    public MUser(String username) {
        this.username = username;
    }
}
