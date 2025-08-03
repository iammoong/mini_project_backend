package project.moonki.dto.muser;

import lombok.Data;

@Data
public class SignupRequestDto {

    private String userId;
    private String username;
    private String password;
    private String nickname;
}
