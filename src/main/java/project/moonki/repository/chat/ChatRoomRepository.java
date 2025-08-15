package project.moonki.repository.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import project.moonki.domain.chat.ChatRoom;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);
}
