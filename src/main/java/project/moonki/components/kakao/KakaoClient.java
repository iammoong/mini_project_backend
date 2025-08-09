package project.moonki.components.kakao;


import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import project.moonki.config.KakaoConfig;
import project.moonki.dto.kakao.KakaoTokenResponse;
import project.moonki.dto.kakao.KakaoUserResponseDto;

@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final KakaoConfig kakao;
    private final RestTemplate rest = new RestTemplate();

    public KakaoTokenResponse exchangeToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakao.getClientId());
        if (kakao.getClientSecret() != null && !kakao.getClientSecret().isBlank()) {
            form.add("client_secret", kakao.getClientSecret());
        }
        form.add("redirect_uri", kakao.getRedirectUri());
        form.add("code", code);

        var entity = new HttpEntity<>(form, headers);
        ResponseEntity<KakaoTokenResponse> res =
                rest.postForEntity(kakao.getTokenUri(), entity, KakaoTokenResponse.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null)
            throw new IllegalStateException("카카오 토큰 교환 실패: " + res.getStatusCode());

        return res.getBody();
    }

    public KakaoUserResponseDto fetchUser(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        var entity = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserResponseDto> res =
                rest.exchange(kakao.getUserinfoUri(), HttpMethod.GET, entity, KakaoUserResponseDto.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null)
            throw new IllegalStateException("카카오 사용자 조회 실패: " + res.getStatusCode());

        return res.getBody();
    }

}
