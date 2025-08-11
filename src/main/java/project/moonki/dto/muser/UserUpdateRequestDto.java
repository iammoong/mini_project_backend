package project.moonki.dto.muser;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateRequestDto {

    // ★ 신규: 아이디 변경 시 사용 (미변경이면 null)
    private String newUserId;
    private String username;
    private String nickname;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    private String phone;

}
