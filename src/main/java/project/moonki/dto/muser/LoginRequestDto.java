package project.moonki.dto.muser;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String userId;
    private String password;
}
