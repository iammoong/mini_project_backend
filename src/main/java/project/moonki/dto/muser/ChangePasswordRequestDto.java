package project.moonki.dto.muser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotBlank(message = "현재 비밀번호를 입력하세요")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력하세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
        // 필요 시 규칙 강화(영문+숫자 조합 등)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\p{Punct}]{8,64}$",
                message = "영문과 숫자를 포함해 8자 이상 입력하세요")
        String newPassword) {


}
