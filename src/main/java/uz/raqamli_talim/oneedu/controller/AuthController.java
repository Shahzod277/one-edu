package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.model.LoginRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.model.UniversityApiUrlsResponse;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;
import uz.raqamli_talim.oneedu.service.*;
import uz.raqamli_talim.oneedu.service.PkceAuthService;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final ClientSystemService clientSystemService;
    private final MyHemisService myHemisService;
    private final EmployeeHemisService employeeHemisService;
    private final DirectOneIdService directOneIdService;
    private final ClientSystemRepository clientSystemRepository;
    private final RsaKeyService rsaKeyService;
    private final HemisAuthConfigService hemisAuthConfigService;
    private final PkceAuthService pkceAuthService;

    /**
     * Login sahifaga redirect (OneID sahifasi o'rniga o'zimizning login page)
     */
    @GetMapping("/{apiKey}")
    public ResponseEntity<Void> getOneIdAdmin(@PathVariable String apiKey) {
        URI loginPage = UriComponentsBuilder
                .fromUriString("/one-id-login.html")
                .queryParam("apiKey", apiKey)
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(loginPage)
                .build();
    }

    /**
     * Direct login — billing mexanizmi orqali (redirect yo'q, server-to-server)
     */
    @Transactional
    @PostMapping("/direct-login")
    public ResponseEntity<?> directLogin(@RequestBody DirectLoginRequest request) {
        if (request.login == null || request.password == null || request.apiKey == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Login, parol va apiKey majburiy"));
        }

        String apiKey = request.apiKey;
        String universityCode = null;

        // my-tutor|380 formatini parse qilish
        if (apiKey.startsWith("my-tutor")) {
            universityCode = apiKey.substring("my-tutor".length());
            apiKey = "my-tutor";
        }

        try {
            // Billing mexanizmi: id.egov.uz → sso.egov.uz → userInfo
            OneIdResponseUserInfo userInfo = directOneIdService.authenticate(
                    request.login, request.password);

            URI redirectUri;

            if ("my-hemis".equalsIgnoreCase(apiKey)) {
                redirectUri = handleMyHemis(userInfo, request.apiKey);
            } else if ("my-tutor".equalsIgnoreCase(apiKey)) {
                redirectUri = handleMyTutor(userInfo, request.apiKey, universityCode);
            } else {
                redirectUri = handleDefault(userInfo, apiKey);
            }

            return ResponseEntity.ok(Map.of("redirectUrl", redirectUri.toString()));

        } catch (Exception e) {
            log.error("DirectLogin xato: apiKey={}, login={}, error={}",
                    apiKey, request.login, e.getMessage());

            String msg = authService.extractHemisErrorMessage(e);
            if (msg.contains("password is incorrect") || msg.contains("User password")) {
                msg = "Login yoki parol noto'g'ri";
            } else if (msg.contains("not found") || msg.contains("User not found")) {
                msg = "Foydalanuvchi topilmadi";
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("message", msg));
        }
    }

    private URI handleMyHemis(OneIdResponseUserInfo userInfo, String apiKey) {
        var clientSystem = clientSystemRepository.findByApiKey("my-hemis")
                .orElseThrow(() -> new NotFoundException("my-hemis tizimi topilmadi"));

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            authService.saveAudit(clientSystem, null, null, true, "Sizga ruxsat yo'q", null);
            throw new NotFoundException("Sizga ruxsat yo'q");
        }

        try {
            UniversityApiUrlsResponse universityApiUrlsResponse =
                    hemisAuthConfigService.getUniversityBaseByPinflStudent(userInfo.getPin());

            var tokens = hemisAuthConfigService.eduIdLogin(
                    userInfo.getPin(), userInfo.getPportNo(), universityApiUrlsResponse);

            authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(),
                    false, null, universityApiUrlsResponse.getCode());

            return UriComponentsBuilder
                    .fromUriString("https://my.hemis.uz/auth/one-id-callback")
                    .queryParam("token", tokens.token())
                    .queryParam("refreshToken", tokens.refreshToken())
                    .queryParam("api_url", tokens.apiUrl())
                    .build(true)
                    .toUri();

        } catch (Exception e) {
            authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(),
                    true, authService.extractHemisErrorMessage(e), null);

            return UriComponentsBuilder
                    .fromUriString("https://my.hemis.uz/auth/notFound")
                    .build(true)
                    .toUri();
        }
    }

    private URI handleMyTutor(OneIdResponseUserInfo userInfo, String apiKey, String universityCode) {
        var clientSystem = clientSystemRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo'q"));

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            authService.saveAudit(clientSystem, null, null, true, "Sizga ruxsat yo'q", null);
            throw new NotFoundException("Sizga ruxsat yo'q");
        }

        try {
            UniversityApiUrlsResponse universityApiUrlsResponse =
                    hemisAuthConfigService.getUniversityBaseByPinflEmployee(
                            userInfo.getPin(), universityCode);

            var tokens = hemisAuthConfigService.eduIdLoginEmployee(
                    userInfo.getPin(), userInfo.getPportNo(), universityApiUrlsResponse, "tutor");

            authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(),
                    false, null, universityApiUrlsResponse.getCode());

            return UriComponentsBuilder
                    .fromUriString("https://tyutor.hemis.uz/auth/one-id-callback")
                    .queryParam("token", tokens.token())
                    .queryParam("refreshToken", tokens.refreshToken())
                    .queryParam("api_url", tokens.apiUrl())
                    .build(true)
                    .toUri();

        } catch (Exception e) {
            authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(),
                    true, authService.extractHemisErrorMessage(e), universityCode);

            return UriComponentsBuilder
                    .fromUriString("https://tyutor.hemis.uz/auth/notFound")
                    .build(true)
                    .toUri();
        }
    }

    private URI handleDefault(OneIdResponseUserInfo userInfo, String apiKey) {
        ClientSystem clientSystem = clientSystemRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo'q"));

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            authService.saveAudit(clientSystem, null, null, true,
                    "Sizga ruxsat yo'q", clientSystem.getOrganization().getCode());
            throw new NotFoundException("Sizga ruxsat yo'q");
        }

        String payload = userInfo.getPin() + "|" + userInfo.getPportNo();
        String encrypted = rsaKeyService.encrypt(clientSystem.getPublicKey(), payload);

        authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(),
                false, null, clientSystem.getOrganization().getCode());

        return UriComponentsBuilder
                .fromUriString(clientSystem.getRedirectUrl())
                .queryParam("data", encrypted)
                .build(true)
                .toUri();
    }

    /**
     * Eski callback — agar OneID redirect ishlasa (zaxira sifatida qoldirildi)
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> oneIdAdminSignIn(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state bo'sh kelib qoldi");
        }

        String system;
        String universityCode = null;

        if (state.contains("|")) {
            String[] parts = state.split("\\|", 2);
            system = parts[0];
            universityCode = parts[1];
        } else {
            system = state;
        }

        URI redirectUri;

        if ("pkce".equalsIgnoreCase(system)) {
            // PKCE flow — universityCode aslida sessionId
            redirectUri = pkceAuthService.handleCallback(code, universityCode);
        } else if ("my-hemis".equalsIgnoreCase(system)) {
            redirectUri = myHemisService.oneIdAdminSignInAndRedirect(code, state);
        } else if ("my-tutor".equalsIgnoreCase(system)) {
            String type = "tutor";
            redirectUri = employeeHemisService.oneIdAdminSignInAndRedirect(
                    code, state, universityCode, type);
        } else {
            redirectUri = authService.oneIdAdminSignInAndRedirect(code, state);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @PostMapping("public/signIn")
    public ResponseEntity<?> signIn(@RequestBody LoginRequest request) {
        ResponseDto response = authService.signIn(request);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getCode()));
    }

    @GetMapping("my-hemis")
    public ResponseEntity<Void> getOneIdAdminMyHemis() {
        URI loginPage = UriComponentsBuilder
                .fromUriString("/one-id-login.html")
                .queryParam("apiKey", "my-hemis")
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(loginPage)
                .build();
    }

    @GetMapping("my-tutor")
    public ResponseEntity<Void> getOneIdAdminEmployee(
            @RequestParam("universityCode") String universityCode
    ) {
        URI loginPage = UriComponentsBuilder
                .fromUriString("/one-id-login.html")
                .queryParam("apiKey", "my-tutor" + universityCode)
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(loginPage)
                .build();
    }

    public record DirectLoginRequest(String login, String password, String apiKey) {}
}
