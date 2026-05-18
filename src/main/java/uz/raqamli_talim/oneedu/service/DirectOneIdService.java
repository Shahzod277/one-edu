package uz.raqamli_talim.oneedu.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DirectOneIdService {

    private static final String SSO_AUTHORIZATION_URL =
            "https://sso.egov.uz/sso/oauth/Authorization.do?response_type=one_code" +
                    "&client_id=edu_hemis&redirect_uri=https://hemis.uz/api/auth/callback" +
                    "&scope=hemis.uz&state=testState";
    private static final String ONEID_LOGIN_URL = "https://id.egov.uz/api/identity/auth/login";
    private static final String ONEID_SSO_GENERATE_URL = "https://id.egov.uz/api/sso/v1/generate";
    private static final String SSO_TOKEN_EXCHANGE_URL = "https://sso.egov.uz/sso/oauth/Authorization.do";

    private static final String CLIENT_ID = "edu_hemis";
    private static final String CLIENT_SECRET = "4j3WLsU3O8OcUdiM8DvgE8o8Fxa00dq6";
    private static final String SCOPE = "edu_hemis";
    private static final String REDIRECT_URI = "https://hemis.uz/api/auth/callback";

    private static final Pattern TOKEN_ID_PATTERN = Pattern.compile("token_id=([a-f0-9-]+)");

    private final WebClient webClient;

    public DirectOneIdService(@Qualifier("insecureWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public OneIdResponseUserInfo authenticate(String login, String password) {
        log.info("DirectOneId: auth boshlandi, login={}", login);

        // 1. sso.egov.uz dan token_id olish
        String tokenId = fetchTokenId();
        log.info("[1] token_id: {}", tokenId);

        // 2. id.egov.uz da login
        String jwt = loginToOneId(login, password);
        log.info("[2] JWT olindi, length={}", jwt.length());

        // 3. id.egov.uz da SSO code generate
        String code = generateSsoCode(tokenId, jwt);
        log.info("[3] SSO code: {}", code);

        // 4. sso.egov.uz da code → access_token
        String accessToken = exchangeCodeForToken(code);
        log.info("[4] access_token: {}", accessToken);

        // 5. sso.egov.uz da access_token → userInfo
        OneIdResponseUserInfo userInfo = getUserInfo(accessToken);
        log.info("[5] pinfl: {}, passport: {}", userInfo.getPin(), userInfo.getPportNo());

        return userInfo;
    }

    private String fetchTokenId() {
        return webClient.get()
                .uri(SSO_AUTHORIZATION_URL)
                .exchangeToMono(response -> {
                    String location = response.headers().asHttpHeaders().getFirst("Location");
                    if (location == null) {
                        return reactor.core.publisher.Mono.error(
                                new RuntimeException("SSO Authorization: Location header yo'q"));
                    }
                    Matcher m = TOKEN_ID_PATTERN.matcher(location);
                    if (!m.find()) {
                        return reactor.core.publisher.Mono.error(
                                new RuntimeException("SSO Authorization: token_id topilmadi: " + location));
                    }
                    return reactor.core.publisher.Mono.just(m.group(1));
                })
                .block();
    }

    private String loginToOneId(String login, String password) {
        LoginResponse resp = webClient.post()
                .uri(ONEID_LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "password", password))
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .block();

        if (resp == null || resp.getToken() == null) {
            throw new RuntimeException("OneID login: token topilmadi");
        }
        return resp.getToken();
    }

    @SuppressWarnings("unchecked")
    private String generateSsoCode(String tokenId, String jwt) {
        Map<String, Object> body = webClient.post()
                .uri(ONEID_SSO_GENERATE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwt)
                .bodyValue(Map.of("uuid", tokenId, "scope", SCOPE))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (body == null || body.get("code") == null) {
            throw new RuntimeException("SSO generate: code topilmadi: " + body);
        }
        return body.get("code").toString();
    }

    @SuppressWarnings("unchecked")
    private String exchangeCodeForToken(String code) {
        String uri = SSO_TOKEN_EXCHANGE_URL +
                "?grant_type=one_authorization_code" +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&code=" + code +
                "&redirect_uri=" + REDIRECT_URI;

        Map<String, Object> body = webClient.post()
                .uri(uri)
                .headers(h -> h.setBasicAuth(CLIENT_ID, CLIENT_SECRET))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (body == null || body.get("access_token") == null) {
            throw new RuntimeException("Token exchange: access_token topilmadi: " + body);
        }
        return body.get("access_token").toString();
    }

    private OneIdResponseUserInfo getUserInfo(String accessToken) {
        String uri = SSO_TOKEN_EXCHANGE_URL +
                "?grant_type=one_access_token_identify" +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&access_token=" + accessToken +
                "&scope=" + SCOPE;

        return webClient.post()
                .uri(uri)
                .headers(h -> h.setBasicAuth(CLIENT_ID, CLIENT_SECRET))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(OneIdResponseUserInfo.class)
                .block();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LoginResponse {
        private String token;
        private String role;
        private int code;
    }
}
