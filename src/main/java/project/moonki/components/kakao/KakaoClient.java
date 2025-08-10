package project.moonki.components.kakao;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import project.moonki.config.KakaoConfig;
import project.moonki.dto.kakao.KakaoTokenResponse;
import project.moonki.dto.kakao.KakaoUserResponseDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final KakaoConfig kakao;
    private final RestTemplate rest = new RestTemplate();


    public KakaoTokenResponse exchangeToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            log.info("[KakaoClient] 토큰 교환 시작: tokenUri={}, redirectUri={}, clientId(head6)={}",
                    kakao.getTokenUri(),
                    kakao.getRedirectUri(),
                    kakao.getClientId() != null ? kakao.getClientId().substring(0, Math.min(6, kakao.getClientId().length())) : "null");

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

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                log.error("[KakaoClient] 토큰 교환 실패(non-2xx): status={}", res.getStatusCode());
                throw new IllegalStateException("카카오 토큰 교환 실패: " + res.getStatusCode());
            }

            log.info("[KakaoClient] 토큰 교환 성공: status={}", res.getStatusCode());
            return res.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("[KakaoClient] 토큰 교환 오류: status={}, body={}", e.getStatusCode(), body);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "카카오 토큰 교환 실패: " + e.getStatusCode() + " body=" + body, e);
        } catch (Exception e) {
            log.error("[KakaoClient] 토큰 교환 일반 예외: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "카카오 토큰 교환 실패: " + e.getMessage(), e);
        }
    }

    public KakaoUserResponseDto fetchUser(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        try {
            log.info("[KakaoClient] 사용자 조회 시작: userinfoUri={}", kakao.getUserinfoUri());

            var entity = new HttpEntity<>(headers);
            ResponseEntity<KakaoUserResponseDto> res =
                    rest.exchange(kakao.getUserinfoUri(), HttpMethod.GET, entity, KakaoUserResponseDto.class);

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                log.error("[KakaoClient] 사용자 조회 실패(non-2xx): status={}", res.getStatusCode());
                throw new IllegalStateException("카카오 사용자 조회 실패: " + res.getStatusCode());
            }

            log.info("[KakaoClient] 사용자 조회 성공: status={}", res.getStatusCode());
            return res.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("[KakaoClient] 사용자 조회 오류: status={}, body={}", e.getStatusCode(), body);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "카카오 사용자 조회 실패: " + e.getStatusCode() + " body=" + body, e);
        } catch (Exception e) {
            log.error("[KakaoClient] 사용자 조회 일반 예외: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "카카오 사용자 조회 실패: " + e.getMessage(), e);
        }
    }
}
