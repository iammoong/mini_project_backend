package project.moonki.repository.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import project.moonki.domain.chat.ChatRead;

import java.util.Optional;

public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {
    Optional<ChatRead> findByRoomIdAndUserId(Long roomId, Long userId);
}
