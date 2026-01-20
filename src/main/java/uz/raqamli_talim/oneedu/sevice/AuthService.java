package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.raqamli_talim.oneedu.domain.Audit;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.domain.Role;
import uz.raqamli_talim.oneedu.domain.User;
import uz.raqamli_talim.oneedu.enums.ResponseMessage;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.model.JwtResponse;
import uz.raqamli_talim.oneedu.model.LoginRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.repository.AuditRepository;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;
import uz.raqamli_talim.oneedu.repository.UserRepository;
import uz.raqamli_talim.oneedu.security.JwtTokenProvider;
import uz.raqamli_talim.oneedu.security.UserDetailsImpl;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

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

    public URI redirectOneIdUrlAdmin(String apiKey) {
        return oneIdServiceApiAdmin.redirectOneIdUrl(apiKey);
    }

    public Mono<URI> oneIdAdminSignInAndRedirect(String code, String apiKey) {

        return Mono.fromCallable(() ->
                        clientSystemRepository.findByApiKey(apiKey)
                                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo‘q"))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(clientSystem -> {

                    if (!Boolean.TRUE.equals(clientSystem.getActive())) {
                        return saveAudit(clientSystem, null, true)
                                .then(Mono.error(new NotFoundException("Sizga ruxsat yo‘q")));
                    }

                    return Mono.fromCallable(() -> {
                                OneIdTokenResponse token = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
                                OneIdResponseUserInfo userInfo = oneIdServiceApiAdmin.getUserInfo(token.getAccess_token());
                                String payload = userInfo.getPin() + "|" + userInfo.getPportNo();
                                String encrypted = rsaKeyService.encrypt(clientSystem.getPublicKey(), payload);

                                URI callbackUri = UriComponentsBuilder
                                        .fromUriString(clientSystem.getRedirectUrl())
                                        .queryParam("data", encrypted)
                                        .build(true)
                                        .toUri();

                                return new Result(callbackUri, userInfo.getPin());
                            })
                            .subscribeOn(Schedulers.boundedElastic()) // ✅ OneID call + encrypt (ehtiyot uchun)
                            .flatMap(res ->
                                    saveAudit(clientSystem, res.pinfl(), false) // ✅ JPA save blocking
                                            .thenReturn(res.uri())
                            )
                            .onErrorResume(e ->
                                    saveAudit(clientSystem, null, true)
                                            .then(Mono.error(e))
                            );
                });
    }

    /** JPA save blocking bo‘lgani uchun boundedElastic’da yozamiz */
    public Mono<Void> saveAudit(ClientSystem clientSystem, String pinfl, boolean error) {
        return Mono.fromRunnable(() -> {
                    Audit audit = new Audit();
                    audit.setClientSystem(clientSystem);
                    audit.setPinfl(pinfl);      // ⚠️ xohlasang hash qilamiz
                    audit.setError(error);
                    auditRepository.save(audit);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /** ichki kichik record */
    record Result(URI uri, String pinfl) {}
    @Transactional
    public ResponseDto signIn(LoginRequest request) {

        User user = userRepository.findActiveUserByPinfl(request.getUsername())
                .orElseThrow(() -> new NotFoundException(ResponseMessage.NOT_FOUND.getMessage()));

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matches) {
            return new ResponseDto(HttpStatus.UNAUTHORIZED.value(), "Login yoki parol noto'g'ri", false);
        }

        Authentication authenticate = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        UserDetailsImpl userDetails = (UserDetailsImpl) authenticate.getPrincipal();
        assert userDetails != null;
        String jwtToken = jwtTokenProvider.generateJWTToken(userDetails);

        JwtResponse jwtResponse = new JwtResponse();
        jwtResponse.setJwtToken(jwtToken);
        jwtResponse.setRoles(user.getRoles().stream().map(Role::getName).toList());

        return new ResponseDto(HttpStatus.OK.value(), ResponseMessage.SUCCESSFULLY.getMessage(), jwtResponse);
    }

}
