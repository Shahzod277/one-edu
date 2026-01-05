package uz.raqamli_talim.oneedu.api_integration.one_id_api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OneIdTokenResponse {

    private String token_type;
    private long expires_in;
    private String access_token;
    private String refresh_token;
    private String scope;
}
