package project.moonki.dto.login;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import project.moonki.dto.muser.UserResponseDto;


@Getter @Setter
public class LoginResponseDto {

    private final UserResponseDto user;
    private final String token;

    private String userId;
    private String username;
    private String email;
    private String kakaoId;

    @Builder
    public LoginResponseDto(UserResponseDto user, String token) {
        this.user = user;
        this.token = token;
    }
}
