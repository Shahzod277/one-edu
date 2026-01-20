package uz.raqamli_talim.oneedu.sevice;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.repository.AuditRepository;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;
import uz.raqamli_talim.oneedu.repository.UserRepository;
import uz.raqamli_talim.oneedu.security.JwtTokenProvider;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyHemisService {
    private final OneIdServiceApiAdmin oneIdServiceApiAdmin;
    private final ClientSystemRepository systemRepository;
    private final ClientSystemRepository clientSystemRepository;
    private final WebClient webClient;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RsaKeyService rsaKeyService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final HemisAuthConfigService hemisAuthConfigService;


    public Mono<URI> oneIdAdminSignInAndRedirect(String code, String apiKey) {

        return Mono.fromCallable(() ->
                        clientSystemRepository.findByApiKey(apiKey)
                                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo‘q"))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(clientSystem -> {

                    if (!Boolean.TRUE.equals(clientSystem.getActive())) {
                        return authService.saveAudit(clientSystem, null, true)
                                .then(Mono.error(new NotFoundException("Sizga ruxsat yo‘q")));
                    }

                    return Mono.fromCallable(() -> {
                                OneIdTokenResponse token = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
                                return oneIdServiceApiAdmin.getUserInfo(token.getAccess_token()); // faqat userInfo qaytaramiz
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(userInfo ->
                                    // ✅ HEMIS edu-id-login dan tokenni olamiz
                                    hemisAuthConfigService.eduIdLogin(userInfo.getPin(), userInfo.getPportNo())
                                            .map(tokens -> {
                                                // ✅ Redirect: my.hemis callback
                                                URI callbackUri = UriComponentsBuilder
                                                        .fromUriString("https://my.hemis.uz/auth/one-id-callback")
                                                        .queryParam("token", tokens.token()) // TokenData record: token()
                                                        // xohlasang refresh ham yubor:
                                                        // .queryParam("refresh_token", tokens.refreshToken())
                                                        .build(true)
                                                        .toUri();
                                                return new AuthService.Result(callbackUri, userInfo.getPin());
                                            })
                            )
                            .flatMap(res ->
                                    authService.saveAudit(clientSystem, res.pinfl(), false)
                                            .thenReturn(res.uri())
                            )
                            .onErrorResume(e ->
                                    authService.saveAudit(clientSystem, null, true)
                                            .then(Mono.error(e))
                            );
                });
    }


}
