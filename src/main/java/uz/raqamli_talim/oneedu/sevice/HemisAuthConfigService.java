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
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

    // ✅ MVC / blocking
    public Boolean setKeys(String privateKey, String apiKey, String universityCode) {

        if (privateKey == null || privateKey.isBlank())
            throw new IllegalArgumentException("privateKey bo'sh");
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalArgumentException("apiKey bo'sh");
        if (universityCode == null || universityCode.isBlank())
            throw new IllegalArgumentException("universityCode bo'sh");

        String baseUrl = getUniversityBaseUrl(universityCode); // blocking
        WebClient wc = WebClient.create(baseUrl);

        AuthKeysBody reqBody = new AuthKeysBody(privateKey, apiKey);

        String jsonBody;
        try {
            jsonBody = om.writeValueAsString(reqBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialize error", e);
        }

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = hmacSha256Hex(timestamp + jsonBody, secret);

        HemisResponse resp = wc.post()
                .uri("/rest/auth-config/set-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .bodyValue(reqBody)
                .retrieve()
                .bodyToMono(HemisResponse.class)
                .block();

        return resp != null && Boolean.TRUE.equals(resp.success);
    }

    // ✅ oldin private Mono edi → endi blocking String
    private String getUniversityBaseUrl(String universityCode) {

        String body = central.get()
                .uri("/rest/v1/public/university-api-urls")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (body == null || body.isBlank())
            throw new RuntimeException("university-api-urls body bo'sh");

        try {
            JsonNode json = om.readTree(body);

            JsonNode data = json.get("data");
            if (data == null || !data.isArray())
                throw new RuntimeException("university-api-urls JSON noto‘g‘ri: " + body);

            for (JsonNode u : data) {
                String code = u.path("code").asText(null);
                String api  = u.path("api_url").asText(null);

                if (api != null && universityCode.equals(code)) {
                    return extractBaseUrl(api);
                }
            }

            throw new RuntimeException("universityCode topilmadi: " + universityCode);

        } catch (Exception e) {
            throw new RuntimeException("university-api-urls parse error. Body=" + body, e);
        }
    }

    // ✅ MVC / blocking
    public UniversityApiUrlsResponse getUniversityBaseByPinfl(String pinfl) {
        if (pinfl == null || pinfl.isBlank())
            throw new IllegalArgumentException("pinfl bo'sh");

        try {
            return stat.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/integration/student/" +
                                    "university-info-pinfl")
                            .queryParam("pinfl", pinfl)
                            .build()
                    )
                    .retrieve()
                    .onStatus(s -> s.value() == 404,
                            r -> r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(b -> new RuntimeException("STAT: student/university topilmadi: pinfl=" + pinfl + " body=" + b)))
                    .onStatus(HttpStatusCode::is5xxServerError,
                            r -> r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(b -> new RuntimeException("STAT tizimida uzilish bor. body=" + b)))
                    .bodyToMono(UniversityApiUrlsResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("STAT HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString(), e);
        }
    }

    // ✅ MVC / blocking
    public TokenData eduIdLoginTest(String pin, String serial) {

        if (pin == null || pin.isBlank())
            throw new IllegalArgumentException("pin bo'sh");
        if (serial == null || serial.isBlank())
            throw new IllegalArgumentException("serial bo'sh");

        pin = pin.trim().replaceAll("\\s+", "");
        serial = serial.trim().replaceAll("\\s+", "").toUpperCase();

        long ts = Instant.now().getEpochSecond();
        String nonce = randomHex16bytes();

        String canonical =
                "PASSPORT_LOGIN\n" +
                        pin + "\n" +
                        serial + "\n" +
                        ts + "\n" +
                        nonce;

        String sigBase64 = hmacSha256Base64(secret, canonical);
        String hash = ts + ":" + nonce + ":" + sigBase64;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("pin", pin);
        form.add("serial", serial);
        form.add("hash", hash);

        UniversityApiUrlsResponse u = getUniversityBaseByPinfl(pin);
        if (u == null)
            throw new RuntimeException("STAT topilmadi: pinfl=" + pin);

        String apiUrl = u.getApi_url();
        if (apiUrl == null || apiUrl.isBlank())
            throw new RuntimeException("STAT api_url bo'sh: pinfl=" + pin);

        String baseUrl = extractBaseUrl(apiUrl);
        WebClient wc = WebClient.create(baseUrl);

        try {
            EduIdLoginResponse resp = wc.post()
                    .uri("/rest/v1/auth/edu-id-login")
                    .header(HttpHeaders.USER_AGENT, "id.edu.uz")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(b -> new RuntimeException(
                                            "HEMIS HTTP " + r.statusCode().value() + " body=" + b
                                    )))
                    .bodyToMono(EduIdLoginResponse.class)
                    .block();

            if (resp == null)
                throw new RuntimeException("HEMIS response null");

            if (!Boolean.TRUE.equals(resp.success))
                throw new RuntimeException(resp.error != null ? resp.error : "HEMIS success=false");

            if (resp.data == null || resp.data.token == null || resp.data.token.isBlank())
                throw new RuntimeException("HEMIS token qaytmadi");

            return new TokenData(
                    resp.data.token,
                    resp.data.refresh_token,
                    apiUrl,
                    baseUrl
            );

        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "HEMIS HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    public TokenData eduIdLogin(String pin, String serial,UniversityApiUrlsResponse u) {

        if (pin == null || pin.isBlank())
            throw new IllegalArgumentException("pin bo'sh");
        if (serial == null || serial.isBlank())
            throw new IllegalArgumentException("serial bo'sh");

        pin = pin.trim().replaceAll("\\s+", "");
        serial = serial.trim().replaceAll("\\s+", "").toUpperCase();

        long ts = Instant.now().getEpochSecond();
        String nonce = randomHex16bytes();

        String canonical =
                "PASSPORT_LOGIN\n" +
                        pin + "\n" +
                        serial + "\n" +
                        ts + "\n" +
                        nonce;

        String sigBase64 = hmacSha256Base64(secret, canonical);
        String hash = ts + ":" + nonce + ":" + sigBase64;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("pin", pin);
        form.add("serial", serial);
        form.add("hash", hash);
        String apiUrl = u.getApi_url();
        if (apiUrl == null || apiUrl.isBlank())
            throw new RuntimeException("STAT api_url bo'sh: pinfl=" + pin);

        String baseUrl = extractBaseUrl(apiUrl);
        WebClient wc = WebClient.create(baseUrl);

        try {
            EduIdLoginResponse resp = wc.post()
                    .uri("/rest/v1/auth/edu-id-login")
                    .header(HttpHeaders.USER_AGENT, "id.edu.uz")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(b -> new RuntimeException(
                                            "HEMIS HTTP " + r.statusCode().value() + " body=" + b
                                    )))
                    .bodyToMono(EduIdLoginResponse.class)
                    .block();

            if (resp == null)
                throw new RuntimeException("HEMIS response null");

            if (!Boolean.TRUE.equals(resp.success))
                throw new RuntimeException(resp.error != null ? resp.error : "HEMIS success=false");

            if (resp.data == null || resp.data.token == null || resp.data.token.isBlank())
                throw new RuntimeException("HEMIS token qaytmadi");

            return new TokenData(
                    resp.data.token,
                    resp.data.refresh_token,
                    apiUrl,
                    baseUrl
            );

        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "HEMIS HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString(),
                    e
            );
        }
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

    public record TokenData(
            String token,
            String refreshToken,
            String apiUrl,
            String baseUrl
    ) {}

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

    private static String urlEncode(String s) {
        return s.replace("%", "%25")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("+", "%2B")
                .replace(" ", "%20");
    }

    private record AuthKeysBody(String private_key, String api_key) {}
}
