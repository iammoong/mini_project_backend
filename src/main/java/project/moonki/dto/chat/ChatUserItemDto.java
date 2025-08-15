package project.moonki.dto.chat;

public record ChatUserItemDto(
        Long id,
        String nickname,
        String username,
        String email,
        Long profileImageId
) {
}
