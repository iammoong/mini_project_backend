package project.moonki.dto.chat;

public record ChatRoomDto(
        Long id,
        Long meId,
        Long otherUserId
) {
}
