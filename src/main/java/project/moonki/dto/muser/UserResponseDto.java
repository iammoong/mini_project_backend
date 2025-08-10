package project.moonki.dto.muser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String userId;
    private String kakaoId;
    private String username;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;
}
