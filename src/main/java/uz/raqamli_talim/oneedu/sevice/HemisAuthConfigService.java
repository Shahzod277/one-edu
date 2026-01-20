package uz.raqamli_talim.oneedu.sevice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.raqamli_talim.oneedu.model.HemisResponse;
import uz.raqamli_talim.oneedu.model.UniversityApiUrlsResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class HemisAuthConfigService {

    private final WebClient central = WebClient.create("https://student.hemis.uz");
    private final WebClient stat    = WebClient.create("https://stat.edu.uz");

    private final String secret = "hG45Jkl934mLk5fFtu387cBi";

    private final ObjectMapper om = new ObjectMapper();

    public Mono<Boolean> setKeys(String privateKey, String apiKey, String universityCode) {

        if (privateKey == null || privateKey.isBlank())
            return Mono.error(new IllegalArgumentException("privateKey bo'sh"));
        if (apiKey == null || apiKey.isBlank())
            return Mono.error(new IllegalArgumentException("apiKey bo'sh"));
        if (universityCode == null || universityCode.isBlank())
            return Mono.error(new IllegalArgumentException("universityCode bo'sh"));

        return getUniversityBaseUrl(universityCode)
                .flatMap(baseUrl -> {

                    WebClient wc = WebClient.create(baseUrl);

                    AuthKeysBody reqBody = new AuthKeysBody(privateKey, apiKey);

                    String jsonBody;
                    try {
                        // ✅ aynan yuboriladigan body'ni serialize qilib olamiz
                        jsonBody = om.writeValueAsString(reqBody);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("JSON serialize error", e));
                    }

                    String timestamp = String.valueOf(Instant.now().getEpochSecond());
                    String signature = hmacSha256Hex(timestamp + jsonBody, secret);

                    return wc.post()
                            .uri("/rest/auth-config/set-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Timestamp", timestamp)
                            .header("X-Signature", signature)
                            .bodyValue(reqBody)
                            .retrieve()
                            .bodyToMono(HemisResponse.class)
                            .map(resp -> resp != null && Boolean.TRUE.equals(resp.success));
                });
    }

    private Mono<String> getUniversityBaseUrl(String universityCode) {
        return central.get()
                .uri("/rest/v1/public/university-api-urls")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        JsonNode json = om.readTree(body);

                        JsonNode data = json.get("data");
                        if (data == null || !data.isArray())
                            return Mono.error(new RuntimeException("university-api-urls JSON noto‘g‘ri: " + body));

                        for (JsonNode u : data) {
                            String code = u.path("code").asText(null);
                            String api  = u.path("api_url").asText(null);

                            if (api != null && universityCode.equals(code)) {
                                return Mono.just(extractBaseUrl(api));
                            }
                        }

                        return Mono.error(new RuntimeException("universityCode topilmadi: " + universityCode));

                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("university-api-urls parse error. Body=" + body, e));
                    }
                });
    }


    public Mono<UniversityApiUrlsResponse> getUniversityBaseByPinfl(String pinfl) {
        if (pinfl == null || pinfl.isBlank())
            return Mono.error(new IllegalArgumentException("pinfl bo'sh"));

        return stat.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/integration/student/university-info-pinfl")
                        .queryParam("pinfl", pinfl)
                        .build()
                )
                .retrieve()
                .onStatus(s -> s.value() == 404,
                        r -> Mono.error(new RuntimeException("STAT: student/university topilmadi: pinfl=" + pinfl)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        r -> Mono.error(new RuntimeException("STAT tizimida uzilish bor")))
                .bodyToMono(UniversityApiUrlsResponse.class);
    }

    public Mono<TokenData> eduIdLogin(String pin, String serial) {

        if (pin == null || pin.isBlank())
            return Mono.error(new IllegalArgumentException("pin bo'sh"));
        if (serial == null || serial.isBlank())
            return Mono.error(new IllegalArgumentException("serial bo'sh"));

        // ✅ normalize (Signer bilan bir xil)
        pin = pin.trim().replaceAll("\\s+", "");
        serial = serial.trim().replaceAll("\\s+", "").toUpperCase();

        long ts = Instant.now().getEpochSecond();
        String nonce = randomHex16bytes(); // 16 bytes -> hex

        // ✅ canonical (Signer bilan 1:1)
        String canonical =
                "PASSPORT_LOGIN\n" +
                        pin + "\n" +
                        serial + "\n" +
                        ts + "\n" +
                        nonce;

        // ✅ sig = Base64(HMAC_SHA256(secret, canonical))
        String sigBase64 = hmacSha256Base64(secret, canonical);

        // ✅ hash = ts:nonce:sig
        String hash = ts + ":" + nonce + ":" + sigBase64;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("pin", pin);
        form.add("serial", serial);
        form.add("hash", hash); // ✅ aynan shu

        String finalPin = pin;

        return getUniversityBaseByPinfl(pin)
                .switchIfEmpty(Mono.error(new RuntimeException("STAT topilmadi: pinfl=" + finalPin)))
                .flatMap(u -> {

                    if (u.getApi_url() == null || u.getApi_url().isBlank())
                        return Mono.error(new RuntimeException("STAT api_url bo'sh: pinfl=" + finalPin));

                    String baseUrl = extractBaseUrl(u.getApi_url());
                    WebClient wc = WebClient.create(baseUrl);

                    return wc.post()
                            .uri("/rest/v1/auth/edu-id-login")
                            .header(HttpHeaders.USER_AGENT, "id.edu.uz")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromFormData(form))
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, r ->
                                    r.bodyToMono(String.class)
                                            .flatMap(b -> Mono.error(new RuntimeException(
                                                    "HEMIS HTTP " + r.statusCode().value() + " body=" + b
                                            )))
                            )
                            .bodyToMono(EduIdLoginResponse.class)
                            .flatMap(resp -> {
                                if (resp == null)
                                    return Mono.error(new RuntimeException("HEMIS response null"));

                                if (!Boolean.TRUE.equals(resp.success))
                                    return Mono.error(new RuntimeException(
                                            resp.error != null ? resp.error : "HEMIS success=false"
                                    ));

                                if (resp.data == null || resp.data.token == null || resp.data.token.isBlank())
                                    return Mono.error(new RuntimeException("HEMIS token qaytmadi"));

                                return Mono.just(new TokenData(resp.data.token, resp.data.refresh_token));
                            });
                });
    }


    private static String randomHex16bytes() {
        byte[] nonceBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(nonceBytes);
        return toHex(nonceBytes);
    }
    private static String hmacSha256Base64(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }


    // ===== response DTO lar =====
    public static class EduIdLoginResponse {
        public Boolean success;
        public String error;
        public EduIdLoginData data;
        public Integer code;
    }

    public static class EduIdLoginData {
        public String token;
        public String refresh_token;
    }

    public record TokenData(String token, String refreshToken) {}

    private static String extractBaseUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) return "";
        try {
            URI uri = URI.create(apiUrl.trim());
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            String s = apiUrl.trim().replaceAll("/+$", "");
            s = s.replace("/rest/v1", "");
            return s.replaceAll("/+$", "");
        }
    }

    // ===== HELPERS =====
    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // form canonical uchun minimal url-encode (oddiy holat uchun yetadi)
    private static String urlEncode(String s) {
        return s.replace("%", "%25")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("+", "%2B")
                .replace(" ", "%20");
    }

    private record AuthKeysBody(String private_key, String api_key) {}
}
