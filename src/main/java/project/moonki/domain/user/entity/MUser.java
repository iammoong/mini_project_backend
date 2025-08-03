package project.moonki.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "m_user")
@Getter @Setter @AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId; // 로그인용 아이디

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    public MUser(String username) {
        this.username = username;
    }
}
