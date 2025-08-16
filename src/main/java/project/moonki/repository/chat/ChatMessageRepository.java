package project.moonki.repository.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import project.moonki.domain.chat.ChatMessage;

public interface ChatMessageRepository  extends JpaRepository<ChatMessage,Long>{
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
    long countByRoomIdAndCreatedAtAfter(Long roomId, java.time.LocalDateTime after);

    long countByRoomIdAndSenderIdAndCreatedAtAfter(Long roomId, Long senderId, java.time.LocalDateTime after);
}
