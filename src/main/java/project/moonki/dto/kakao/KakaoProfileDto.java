package project.moonki.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoProfileDto {
    private String nickname;

    @JsonProperty("profile_image_url")
    private String profileImageUrl;
}
