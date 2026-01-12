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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RsaKeyService rsaKeyService;
    private final JwtTokenProvider jwtTokenProvider;

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

        // ✅ bitta payload
        String payload = userInfo.getPin() + "|" + userInfo.getPportNo(); // pinfl|passport

        // ✅ client public key bilan shifrlaymiz
        String encrypted = rsaKeyService.encrypt(clientSystem.getPublicKey(), payload);

        // ✅ endi bitta param yuboriladi
        URI callbackUri = UriComponentsBuilder
                .fromUriString(clientSystem.getPostCallbackUrl())
                .queryParam("data", encrypted)
                .build(true)
                .toUri();

        return Mono.just(callbackUri);
    }


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
