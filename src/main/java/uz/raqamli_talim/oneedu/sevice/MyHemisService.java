package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyHemisService {

    private final OneIdServiceApiAdmin oneIdServiceApiAdmin;
    private final ClientSystemRepository clientSystemRepository;
    private final AuthService authService;
    private final HemisAuthConfigService hemisAuthConfigService;

    @Transactional
    public Mono<URI> oneIdAdminSignInAndRedirect(String code, String apiKey) {

        return Mono.fromCallable(() ->
                        clientSystemRepository.findByApiKey(apiKey)
                                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo‘q"))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(clientSystem -> {

                    if (!Boolean.TRUE.equals(clientSystem.getActive())) {
                        return authService.saveAudit(clientSystem, null, true, "Sizga ruxsat yo‘q")
                                .then(Mono.error(new NotFoundException("Sizga ruxsat yo‘q")));
                    }

                    return Mono.fromCallable(() -> {
                                OneIdTokenResponse token = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
                                return oneIdServiceApiAdmin.getUserInfo(token.getAccess_token());
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(userInfo ->
                                    hemisAuthConfigService.eduIdLogin(userInfo.getPin(), userInfo.getPportNo())
                                            .map(tokens -> {
                                                URI callbackUri = UriComponentsBuilder
                                                        .fromUriString("https://my.hemis.uz/auth/one-id-callback")
                                                        .queryParam("token", tokens.token())
                                                        .queryParam("api_url", tokens.apiUrl())
                                                        .build(true)
                                                        .toUri();

                                                return new AuthService.Result(callbackUri, userInfo.getPin());
                                            })
                            )
                            .flatMap(res ->
                                    authService.saveAudit(clientSystem, res.pinfl(), false)
                                            .thenReturn(res.uri())
                            )
                            .onErrorResume(e -> {
                                // ✅ HEMIS error message ni olib auditga yozamiz
                                String msg = authService.extractHemisErrorMessage(e);

                                // Variant-1: eski holat (exception qaytarish)
                                // return authService.saveAudit(clientSystem, null, true, msg).then(Mono.error(e));

                                // ✅ Variant-2: siz aytgandek error pagega redirect
                                return authService.saveAudit(clientSystem, null, true, msg)
                                        .thenReturn(UriComponentsBuilder
                                                .fromUriString("https://my.hemis.uz/auth/notFound")
                                                .build(true)
                                                .toUri());
                            });
                });
    }
}
