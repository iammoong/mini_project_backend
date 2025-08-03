package project.moonki.dto.muser;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDto {
    private Long id;
    private String userId;
    private String username;
    private String nickname;
    private LocalDateTime createdAt;
}
