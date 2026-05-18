package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.service.PkceAuthService;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/pkce")
@RequiredArgsConstructor
public class PkceAuthController {

    private final PkceAuthService pkceAuthService;

    /**
     * 1-bosqich: Mobil ilova shu endpoint'ga so'rov yuboradi
     * GET /api/auth/pkce/authorize?code_challenge=xxx
     *
     * → One-ID login sahifaga redirect qiladi
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("redirect_uri") String redirectUri
    ) {
        URI oneIdUrl = pkceAuthService.startPkceFlow(codeChallenge, redirectUri);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(oneIdUrl)
                .build();
    }

    /**
     * 2-bosqich: One-ID callback — bu endpoint'ni One-ID o'zi chaqiradi
     * Hozirgi /api/auth/callback ichidan state=pkce|sessionId bo'lsa shu yerga yo'naltiriladi
     * (AuthController.callback dan chaqiriladi)
     */

    /**
     * 3-bosqich: Mobil ilova code_verifier bilan token oladi
     * POST /api/auth/pkce/token
     * { "session_id": "abc", "code_verifier": "xxx" }
     *
     * → JSON: { token, refreshToken, api_url }
     */
    @PostMapping("/token")
    public ResponseEntity<?> exchangeToken(@RequestBody PkceTokenRequest request) {
        try {
            Map<String, Object> tokens = pkceAuthService.exchangeToken(
                    request.sessionId(), request.codeVerifier());

            return ResponseEntity.ok(new ResponseDto(200, "Muvaffaqiyatli", true, tokens));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto(401, e.getMessage(), false));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ResponseDto(400, e.getMessage(), false));
        }
    }

    public record PkceTokenRequest(String sessionId, String codeVerifier) {}
}