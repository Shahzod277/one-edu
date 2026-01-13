package uz.raqamli_talim.oneedu.sevice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.raqamli_talim.oneedu.model.HemisResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class HemisAuthConfigService {

    // 1) Universitetlar ro'yxatini beradigan markaziy endpoint
    private final WebClient central = WebClient.create("https://student.hemis.uz");

    // 2) Secret (o'zing qo'yasan)
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
                .bodyToMono(UniversityApiUrlsResponse.class)
                .flatMap(resp -> {
                    if (resp == null || resp.data == null)
                        return Mono.error(new RuntimeException("university-api-urls response bo'sh"));

                    return resp.data.stream()
                            .filter(u -> universityCode.equals(u.code))
                            .findFirst()
                            .map(u -> Mono.just(extractBaseUrl(u.api_url)))
                            .orElseGet(() ->
                                    Mono.error(new RuntimeException("universityCode topilmadi: " + universityCode))
                            );
                });
    }

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


    // ===== JSON response DTO =====
    public static class UniversityApiUrlsResponse {
        public Boolean success;
        public String error;
        public java.util.List<UniversityItem> data;
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
