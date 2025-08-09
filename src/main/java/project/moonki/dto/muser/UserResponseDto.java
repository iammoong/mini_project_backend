package project.moonki.dto.muser;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String userId;
    private String kakaoId;
    private String username;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;
}
