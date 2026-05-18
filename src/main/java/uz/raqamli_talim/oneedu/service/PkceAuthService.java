package uz.raqamli_talim.oneedu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.model.UniversityApiUrlsResponse;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PkceAuthService {

    private final OneIdServiceApiAdmin oneIdServiceApiAdmin;
    private final ClientSystemRepository clientSystemRepository;
    private final AuthService authService;
    private final HemisAuthConfigService hemisAuthConfigService;
    private final PkceSessionStore sessionStore;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 1-bosqich: Mobil ilova code_challenge yuboradi → One-ID ga redirect
     */
    public URI startPkceFlow(String codeChallenge, String redirectUri) {
        if (codeChallenge == null || codeChallenge.isBlank()) {
            throw new IllegalArgumentException("code_challenge majburiy");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalArgumentException("redirect_uri majburiy");
        }

        String sessionId = generateSessionId();
        String state = "pkce|" + sessionId;

        sessionStore.save(sessionId, codeChallenge, state, redirectUri);

        return oneIdServiceApiAdmin.redirectOneIdUrl(state);
    }

    /**
     * 2-bosqich: One-ID callback — code bilan HEMIS tokenlarni olib, saqlash
     */
    @Transactional
    public URI handleCallback(String code, String sessionId) {
        PkceSessionStore.PkceSession session = sessionStore.get(sessionId);
        if (session == null) {
            throw new NotFoundException("Session topilmadi yoki muddati tugagan");
        }

        var clientSystem = clientSystemRepository.findByApiKey("my-hemis")
                .orElseThrow(() -> new NotFoundException("my-hemis tizimi topilmadi"));

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            authService.saveAudit(clientSystem, null, null, true, "Sizga ruxsat yo'q", null);
            throw new NotFoundException("Sizga ruxsat yo'q");
        }

        try {
            OneIdTokenResponse token = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
            OneIdResponseUserInfo userInfo = oneIdServiceApiAdmin.getUserInfo(token.getAccess_token());

            UniversityApiUrlsResponse university =
                    hemisAuthConfigService.getUniversityBaseByPinflStudent(userInfo.getPin());

            var tokens = hemisAuthConfigService.eduIdLogin(
                    userInfo.getPin(), userInfo.getPportNo(), university);

            authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(),
                    false, null, university.getCode());

            sessionStore.updateWithResult(sessionId,
                    new PkceSessionStore.HemisTokenResult(
                            tokens.token(), tokens.refreshToken(), tokens.apiUrl(), null));

        } catch (Exception e) {
            String msg = authService.extractHemisErrorMessage(e);
            log.error("PKCE callback xato: sessionId={}, error={}", sessionId, msg);

            sessionStore.updateWithResult(sessionId,
                    new PkceSessionStore.HemisTokenResult(null, null, null, msg));
        }

        String redirectUri = session.redirectUri();
        String separator = redirectUri.contains("?") ? "&" : "?";
        return URI.create(redirectUri + separator + "session_id=" + sessionId);
    }

    /**
     * 3-bosqich: Mobil ilova code_verifier bilan token oladi
     */
    public Map<String, Object> exchangeToken(String sessionId, String codeVerifier) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("session_id majburiy");
        }
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("code_verifier majburiy");
        }

        PkceSessionStore.PkceSession session = sessionStore.get(sessionId);
        if (session == null) {
            throw new NotFoundException("Session topilmadi yoki muddati tugagan");
        }

        // PKCE tekshiruv: SHA256(code_verifier) == saqlangan code_challenge
        String computedChallenge = computeCodeChallenge(codeVerifier);
        if (!computedChallenge.equals(session.codeChallenge())) {
            sessionStore.remove(sessionId);
            throw new SecurityException("code_verifier noto'g'ri");
        }

        PkceSessionStore.HemisTokenResult result = session.tokenResult();
        if (result == null) {
            throw new NotFoundException("Token hali tayyor emas, callback kutilmoqda");
        }

        // Bir martalik — session o'chiriladi
        sessionStore.remove(sessionId);

        if (result.error() != null) {
            throw new RuntimeException(result.error());
        }

        return Map.of(
                "token", result.token(),
                "refreshToken", result.refreshToken(),
                "api_url", result.apiUrl()
        );
    }

    private String generateSessionId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String computeCodeChallenge(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 xato", e);
        }
    }
}
