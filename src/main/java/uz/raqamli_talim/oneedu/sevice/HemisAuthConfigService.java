package uz.raqamli_talim.oneedu.sevice;

import com.fasterxml.jackson.databind.JsonNode;
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

@Service
public class HemisAuthConfigService {

    // HEMIS markaziy endpoint (EduID login va public ro‘yxatlar shu yerdan)
    private final WebClient central = WebClient.create("https://student.hemis.uz");

    // STAT endpoint (PINFL -> university info)
    private final WebClient stat = WebClient.create("https://stat.edu.uz");

    // Secret (o'zing qo'yasan)
    private final String secret = "hG45Jkl934mLk5fFtu387cBi";

    public Mono<Boolean> setKeys(String privateKey, String apiKey, String universityCode) {

        if (privateKey == null || privateKey.isBlank())
            return Mono.error(new IllegalArgumentException("privateKey bo'sh"));

        if (apiKey == null || apiKey.isBlank())
            return Mono.error(new IllegalArgumentException("apiKey bo'sh"));

        if (universityCode == null || universityCode.isBlank())
            return Mono.error(new IllegalArgumentException("universityCode bo'sh"));

        return getUniversityBaseUrl(universityCode)
                .flatMap(baseUrl -> {
                    System.out.println(baseUrl);
                    WebClient wc = WebClient.create(baseUrl);

                    String body = "{\"private_key\":\"" + jsonEscape(privateKey) +
                            "\",\"api_key\":\"" + jsonEscape(apiKey) + "\"}";

                    String timestamp = String.valueOf(Instant.now().getEpochSecond());
                    String signature = hmacSha256Hex(timestamp + body, secret);

                    return wc.post()
                            .uri("/rest/auth-config/set-keys")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .header("X-Timestamp", timestamp)
                            .header("X-Signature", signature)
                            .bodyValue(new AuthKeysBody(privateKey, apiKey))
                            .retrieve()
                            .bodyToMono(HemisResponse.class)
                            .map(resp -> Boolean.TRUE.equals(resp.success));
                });
    }

    /**
     * universityCode bo'yicha api_url topib, baseUrl qaytaradi.
     * api_url: https://student.arbu-edu.uz/rest/v1/  -> baseUrl: https://student.arbu-edu.uz
     */
    private Mono<String> getUniversityBaseUrl(String universityCode) {

        return central.get()
                .uri("/rest/v1/public/university-api-urls")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {

                    JsonNode data = json.get("data");
                    if (data == null || !data.isArray()) {
                        return Mono.error(new RuntimeException("university-api-urls response bo‘sh"));
                    }

                    for (JsonNode u : data) {
                        if (universityCode.equals(u.get("code").asText())) {
                            String apiUrl = u.get("api_url").asText();
                            return Mono.just(extractBaseUrl(apiUrl));
                        }
                    }

                    return Mono.error(new RuntimeException(
                            "universityCode topilmadi: " + universityCode
                    ));
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
                .onStatus(
                        s -> s.value() == 404,
                        r -> Mono.error(new RuntimeException("STAT: student/university topilmadi: pinfl=" + pinfl))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        r -> Mono.error(new RuntimeException("STAT tizimida uzilish bor"))
                )
                .bodyToMono(UniversityApiUrlsResponse.class);
    }
    public Mono<TokenData> eduIdLogin( String pin, String serial) {


        if (pin == null || pin.isBlank())
            return Mono.error(new IllegalArgumentException("pin bo'sh"));

        if (serial == null || serial.isBlank())
            return Mono.error(new IllegalArgumentException("serial bo'sh"));

        String body = "{\"pin\":\"" + jsonEscape(pin) +
                "\",\"serial\":\"" + jsonEscape(serial) + "\"}";

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = hmacSha256Hex(timestamp + body, secret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("pin", pin);
        form.add("serial", serial);
        form.add("hash", signature);

        // ✅ PINFL bo‘yicha STATdan university info olamiz
        return getUniversityBaseByPinfl(pin)
                .switchIfEmpty(Mono.error(new RuntimeException("STAT topilmadi: pinfl=" + pin)))
                .flatMap(u -> {
                    if (u.getApi_url() == null || u.getApi_url().isBlank())
                        return Mono.error(new RuntimeException("STAT api_url bo'sh: pinfl=" + pin));

                    String baseUrl = extractBaseUrl(u.getApi_url());
                    WebClient wc = WebClient.create(baseUrl);

                    return wc.post()
                            .uri("/rest/v1/auth/edu-id-login")
                            .header(HttpHeaders.USER_AGENT, "id.edu.uz")
                            .header("X-Timestamp", timestamp)
                            .header("X-Signature", signature)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromFormData(form))
                            .retrieve()
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

    // ===== sizga kerak bo‘ladigan natija =====
    public record TokenData(String token, String refreshToken) {}


    private static String extractBaseUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) return "";
        try {
            URI uri = URI.create(apiUrl.trim());
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            return origin;
        } catch (Exception e) {
            String s = apiUrl.trim().replaceAll("/+$", "");
            s = s.replace("/rest/v1", "");
            return s.replaceAll("/+$", "");
        }
    }


    public static class UniversityItem {
        public String code;
        public String name;
        public Long tin;
        public String api_url;
        public String student_url;
        public String employee_url;
        public String university_type;
        public String version_type;
    }

    // ===== HELPERS =====
    private static String jsonEscape(String s) {
        return s == null ? "" : s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
    private record AuthKeysBody(String private_key, String api_key) {}
}
