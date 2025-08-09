package project.moonki.dto.kakao;

import lombok.Data;

@Data
public class KakaoAccountDto {
    private KakaoProfileDto profile;
    private String email;  // 동의하지 않으면 null
}
