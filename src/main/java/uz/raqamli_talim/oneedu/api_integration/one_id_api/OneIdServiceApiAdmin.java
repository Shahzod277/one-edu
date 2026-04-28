package uz.raqamli_talim.oneedu.api_integration.one_id_api;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class OneIdServiceApiAdmin {

    @Value("${one-id.admin.client-id}")
    private String client_id;
    @Value("${one-id.admin.scope}")
    private String scope;
    @Value("${one-id.admin.redirect-uri}")
    private String redirect_uri;
    @Value("${one-id.admin.client-secret}")
    private String client_secret;
    private final WebClient webClient;

    public URI redirectOneIdUrl(String apiKey) {

        String response_type = "one_code";

        String oneIdUrl =
                "https://sso.egov.uz/sso/oauth/Authorization.do?" +
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
                .uri("https://sso.egov.uz/sso/oauth/Authorization.do?" +
                        "grant_type=" + grant_type +
                        "&client_id=" + client_id +
                        "&client_secret=" + client_secret +
                        "&code=" + code +
                        "&redirect_uri=" + redirect_uri)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(client_id, client_secret))
                .retrieve()
                .bodyToMono(OneIdTokenResponse.class)
                .block();
    }

    public OneIdResponseUserInfo getUserInfo(String accessToken) {
        String grant_type = "one_access_token_identify";
        return webClient.post()
                .uri("https://sso.egov.uz/sso/oauth/Authorization.do?" +
                        "grant_type=" + grant_type +
                        "&client_id=" + client_id +
                        "&client_secret=" + client_secret +
                        "&access_token=" + accessToken +
                        "&scope=" + scope)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(OneIdResponseUserInfo.class)
                .block();
    }


}
