package project.moonki.service.login;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import project.moonki.config.KakaoConfig;
import project.moonki.dto.kakao.KakaoUserDto;
import project.moonki.dto.kakao.KakaoUserResponseDto;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final KakaoConfig kakaoConfig;

    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoConfig.getClientId());
        if (kakaoConfig.getClientSecret() != null && !kakaoConfig.getClientSecret().isBlank()) {
            params.add("client_secret", kakaoConfig.getClientSecret());
        }
        params.add("redirect_uri", kakaoConfig.getRedirectUri());
        params.add("code", code);

        var entity = new HttpEntity<>(params, headers);
        var rest = new RestTemplate();
        ResponseEntity<Map> res = rest.postForEntity(kakaoConfig.getTokenUri(), entity, Map.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null)
            throw new RuntimeException("카카오 토큰 요청 실패: " + res.getStatusCode());

        return (String) res.getBody().get("access_token");
    }

    public KakaoUserDto getKakaoUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        var entity = new HttpEntity<>(headers);
        var rest = new RestTemplate();

        ResponseEntity<KakaoUserResponseDto> res =
                rest.exchange(kakaoConfig.getUserinfoUri(), HttpMethod.GET, entity, KakaoUserResponseDto.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null)
            throw new RuntimeException("카카오 유저 정보 조회 실패: " + res.getStatusCode());

        var body = res.getBody();

        // 프로젝트 내부에서 쓰는 간단 DTO로 변환
        KakaoUserDto dto = new KakaoUserDto();
        dto.setId(String.valueOf(body.getId()));  // MUser.kakaoId가 String이므로 String으로 저장
        if (body.getKakao_account() != null) {
            dto.setEmail(body.getKakao_account().getEmail());
            if (body.getKakao_account().getProfile() != null) {
                dto.setNickname(body.getKakao_account().getProfile().getNickname());
            }
        }
        return dto;
    }

}
