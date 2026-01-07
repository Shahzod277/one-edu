package uz.raqamli_talim.oneedu.api_integration.one_id_api;


import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class OneIdServiceApiAdmin {

    private final String client_id = "my_edu_uz";
    private final String scope = "id.edu.uz";
    private final String redirect_uri = "https://id.edu.uz/api/auth/callback";
    private final String client_secret = "F9o57w9kS7wWlBqqPmSXHztf";
    public static final String ONE_ID_LOGIN = "my_edu_uz";
    public static final String ONE_ID_PASSWORD = "F9o57w9kS7wWlBqqPmSXHztf";
    private final WebClient webClient;

    public URI redirectOneIdUrl(String apiKey) {

        String response_type = "one_code";

        String oneIdUrl =
                "https://sso-cloud.egov.uz/sso/oauth/Authorization.do?" +
                        "state=" + apiKey +                 // ⬅️ ENG MUHIM JOY
                        "&response_type=" + response_type +
                        "&client_id=" + client_id +
                        "&scope=" + scope +
                        "&redirect_uri=" + redirect_uri;

        return URI.create(oneIdUrl);
    }


    public OneIdTokenResponse getAccessAndRefreshToken(String code) {
        String grant_type = "one_authorization_code";
        return webClient.post()
                .uri("https://sso-cloud.egov.uz/sso/oauth/Authorization.do?" +
                        "grant_type=" + grant_type +
                        "&client_id=" + client_id +
                        "&client_secret=" + client_secret +
                        "&code=" + code +
                        "&redirect_uri=" + redirect_uri)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(ONE_ID_LOGIN, ONE_ID_PASSWORD))
                .retrieve()
                .bodyToMono(OneIdTokenResponse.class)
                .block();
    }

    public OneIdResponseUserInfo getUserInfo(String accessToken) {
        String grant_type = "one_access_token_identify";
        return webClient.post()
                .uri("https://sso-cloud.egov.uz/sso/oauth/Authorization.do?" +
                        "grant_type=" + grant_type +
                        "&client_id=" + client_id +
                        "&client_secret=" + client_secret +
                        "&access_token=" + accessToken +
                        "&scope=" + scope)
//                .headers(httpHeaders -> httpHeaders.setBasicAuth(ONE_ID_LOGIN, ONE_ID_PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(OneIdResponseUserInfo.class)
                .block();
    }


}
