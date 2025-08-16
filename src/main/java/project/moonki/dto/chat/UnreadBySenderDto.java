package project.moonki.dto.chat;

public record UnreadBySenderDto(
        Long userId,
        Long count
) {
}
