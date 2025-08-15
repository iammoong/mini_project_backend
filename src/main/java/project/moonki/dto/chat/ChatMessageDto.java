package project.moonki.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageDto(
        Long id,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt
) {
}
