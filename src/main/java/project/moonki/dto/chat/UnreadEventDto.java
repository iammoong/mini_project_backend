package project.moonki.dto.chat;

public record UnreadEventDto(
        String type,
        long total,
        long roomId,
        long senderId,
        long bySender) {
}
