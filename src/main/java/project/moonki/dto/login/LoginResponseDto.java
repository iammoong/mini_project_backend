package project.moonki.dto.login;

import project.moonki.dto.muser.UserResponseDto;

public class LoginResponseDto {

    private final UserResponseDto user;
    private final String token;

    public LoginResponseDto(UserResponseDto user, String token) {
        this.user = user;
        this.token = token;
    }
    public UserResponseDto getUser() { return user; }
    public String getToken() { return token; }
}
