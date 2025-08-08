package project.moonki.service.login;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import project.moonki.config.KakaoConfig;
import project.moonki.dto.KakaoUserDto;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final KakaoConfig kakaoConfig;

    public String getAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoConfig.getRestApiKey());
        params.add("redirect_uri", kakaoConfig.getRedirectUri());
        params.add("code", code);

        HttpEntity<?> entity = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        } else {
            System.out.println("카카오 토큰 요청 실패: " + response.getStatusCode() + " / " + response.getBody());
            throw new RuntimeException("카카오 토큰 요청 실패: " + response.getStatusCode());
        }
    }

    public KakaoUserDto getKakaoUserInfo(String accessToken) {
        String apiUrl = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            System.out.println("카카오 유저 정보 조회 실패: " + response.getStatusCode() + " / " + response.getBody());
            throw new RuntimeException("카카오 유저 정보 조회 실패: " + response.getStatusCode());
        }

        Map<String, Object> kakaoAccount = (Map<String, Object>) response.getBody().get("kakao_account");

        KakaoUserDto dto = new KakaoUserDto();
        dto.setEmail((String) kakaoAccount.get("email"));
        dto.setNickname((String) ((Map) kakaoAccount.get("profile")).get("nickname"));
        dto.setId(String.valueOf(response.getBody().get("id")));
        return dto;
    }
}
