package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.enums.ResponseMessage;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final OneIdServiceApiAdmin oneIdServiceApiAdmin;
    private final ClientSystemRepository systemRepository;
    private final ClientSystemRepository clientSystemRepository;
    private final WebClient webClient;

    public URI redirectOneIdUrlAdmin(String apiKey) {
        return oneIdServiceApiAdmin.redirectOneIdUrl(apiKey);
    }

    @Transactional
    public URI oneIdAdminSignInAndRedirect(String code, String apiKey) {

        ClientSystem clientSystem = clientSystemRepository
                .findByApiKey(apiKey)
                .orElseThrow(() ->
                        new NotFoundException("Sizga ruxsat yo‘q")
                );

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            throw new NotFoundException("Sizga ruxsat yo‘q");
        }
        OneIdTokenResponse oneIdToken =
                oneIdServiceApiAdmin.getAccessAndRefreshToken(code);

        OneIdResponseUserInfo userInfo =
                oneIdServiceApiAdmin.getUserInfo(oneIdToken.getAccess_token());

        if (userInfo == null || userInfo.getPin() == null) {
            return URI.create(clientSystem.getApiKey());
        }

        // 🔥 POST callback
        webClient.post()
                .uri(clientSystem.getPostCallbackUrl())
//                .header("X-API-KEY", clientSystem.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userInfo)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        // 🔁 QAYERGA REDIRECT QILISHNI SERVICE HAL QILADI
        return URI.create(clientSystem.getRedirectUrl());
    }


}
