package project.moonki.domain.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ChatRoom dm(Long a, Long b) {
        // 정렬하여 유니크 보장
        long u1 = Math.min(a, b);
        long u2 = Math.max(a, b);
        return ChatRoom.builder()
                .user1Id(u1)
                .user2Id(u2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public boolean hasParticipant(Long uid) {
        return user1Id.equals(uid) || user2Id.equals(uid);
    }

    public Long otherOf(Long me) {
        if (user1Id.equals(me)) return user2Id;
        if (user2Id.equals(me)) return user1Id;
        return null;

    }
}
