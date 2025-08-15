package project.moonki.domain.chat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_read", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="room_id", nullable = false)
    private Long roomId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name="last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @PrePersist
    public void prePersist() {
        if (lastReadAt == null) {
            lastReadAt = LocalDateTime.now(); // 또는 MYSQL_SAFE_MIN
        }
    }
}
