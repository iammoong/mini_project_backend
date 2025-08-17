package project.moonki.dto.chat;

public record ChatRoomListItemDto(
        Long id,
        ChatUserItemDto other,
        ChatMessageDto lastMessage,
        Long unread
) {
}
