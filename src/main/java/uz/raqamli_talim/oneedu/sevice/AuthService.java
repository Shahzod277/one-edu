package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
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

    public Mono<URI> oneIdAdminSignInAndRedirect(String code, String apiKey) {

        ClientSystem clientSystem = clientSystemRepository
                .findByApiKey(apiKey)
                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo‘q"));

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            throw new NotFoundException("Sizga ruxsat yo‘q");
        }

        OneIdTokenResponse oneIdToken = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
        OneIdResponseUserInfo userInfo = oneIdServiceApiAdmin.getUserInfo(oneIdToken.getAccess_token());

        if (userInfo == null || userInfo.getPin() == null) {
            return Mono.just(URI.create(clientSystem.getRedirectUrl()));
        }

        URI callbackUri = UriComponentsBuilder
                .fromUriString(clientSystem.getPostCallbackUrl()) // https://stat.edu.uz/api/auth-user/callback
                .queryParam("pinfl", userInfo.getPin())
                .queryParam("serialNumber", userInfo.getPportNo())
                .build(true)
                .toUri();

        return webClient.get()
                .uri(callbackUri)
                // .header("X-API-KEY", clientSystem.getApiKey())
                .retrieve()
                .toBodilessEntity()
                .thenReturn(URI.create(clientSystem.getRedirectUrl()));

    }



}
