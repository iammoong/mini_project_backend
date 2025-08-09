package project.moonki.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoConfig {
    private String clientId;
    private String clientSecret;   // optional
    private String redirectUri;
    private String authorizeUri;
    private String tokenUri;
    private String userinfoUri;
}
