package uz.raqamli_talim.oneedu.sevice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.domain.Audit;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.domain.Role;
import uz.raqamli_talim.oneedu.domain.User;
import uz.raqamli_talim.oneedu.enums.ResponseMessage;
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
    private final ClientSystemRepository systemRepository;         // o'zi ishlatilmasa ham o'zgartirmadim
    private final ClientSystemRepository clientSystemRepository;
    private final WebClient webClient;                             // ishlatilmasa ham o'zgartirmadim
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RsaKeyService rsaKeyService;
    private final JwtTokenProvider jwtTokenProvider;

    public URI redirectOneIdUrlAdmin(String apiKey) {
        return oneIdServiceApiAdmin.redirectOneIdUrl(apiKey);
    }

    // ✅ MVC / blocking
    public URI oneIdAdminSignInAndRedirect(String code, String apiKey) {

        ClientSystem clientSystem = clientSystemRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo‘q"));
        var universityCode = new Object[]{null}; // (Java var uchun kichik hack)


        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            saveAudit(clientSystem, null, null, true, "Sizga ruxsat yo‘q",clientSystem.getOrganization().getCode());
            throw new NotFoundException("Sizga ruxsat yo‘q");
        }

        OneIdResponseUserInfo userInfo = null; // ✅ catchda ham ishlatish uchun

        try {
            OneIdTokenResponse token = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
            userInfo = oneIdServiceApiAdmin.getUserInfo(token.getAccess_token());

            String payload = userInfo.getPin() + "|" + userInfo.getPportNo();
            String encrypted = rsaKeyService.encrypt(clientSystem.getPublicKey(), payload);

            URI callbackUri = UriComponentsBuilder
                    .fromUriString(clientSystem.getRedirectUrl())
                    .queryParam("data", encrypted)
                    .build(true)
                    .toUri();

            // ✅ success audit
            saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(), false, null,clientSystem.getOrganization().getCode());

            return callbackUri;

        } catch (Exception e) {
            String msg = extractHemisErrorMessage(e);

            // ✅ userInfo olinib ulgurgan bo‘lsa — auditga yozamiz
            String pin = (userInfo != null) ? userInfo.getPin() : null;
            String serial = (userInfo != null) ? userInfo.getPportNo() : null;

            saveAudit(clientSystem, pin, serial, true, msg,null);

            // ✅ eski behavior: errorni yuqoriga otish
            RuntimeException re = (RuntimeException) e;
            throw re;
        }
    }


    // ✅ MyHemisService da ishlatiladigan bo‘lgani uchun PUBLIC
    public String extractHemisErrorMessage(Throwable e) {
        if (e instanceof WebClientResponseException wex) {
            String body = wex.getResponseBodyAsString();

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(body);

                if (node.hasNonNull("error")) {
                    return node.get("error").asText();
                }
            } catch (Exception ignored) {}

            return "HEMIS HTTP " + wex.getStatusCode().value() + " body=" + body;
        }
        return e.getMessage();
    }


    /** ✅ JPA save blocking bo‘lgani uchun oddiy qilib yozdik */
    public void saveAudit(ClientSystem clientSystem, String pinfl, String serialNumber, boolean error, String errorMessage,String universityCode) {
        Audit audit = new Audit();
        audit.setClientSystem(clientSystem);
        audit.setPinfl(pinfl);
        audit.setSerialNumber(serialNumber);
        audit.setUniversityCode(universityCode);
        audit.setError(error);

        // sizning entity setter'ingiz typo bo‘lishi mumkin — o‘sha holatda qoldirdim
        audit.setErrorMassage(errorMessage);

        auditRepository.save(audit);
    }

    /** ichki kichik record */
    public record Result(URI uri, String pinfl) {}

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
        String jwtToken = jwtTokenProvider.generateJWTToken(userDetails);

        JwtResponse jwtResponse = new JwtResponse();
        jwtResponse.setJwtToken(jwtToken);
        jwtResponse.setRoles(user.getRoles().stream().map(Role::getName).toList());

        return new ResponseDto(HttpStatus.OK.value(), ResponseMessage.SUCCESSFULLY.getMessage(), jwtResponse);
    }
}
