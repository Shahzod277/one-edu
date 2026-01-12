package uz.raqamli_talim.oneedu.sevice;

import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class RsaKeyService {


    private static final SecureRandom RNG = new SecureRandom();

    public  String generateApiKey() {
        byte[] bytes = new byte[32]; // 256-bit
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    public RsaKeys generateRSA() {
        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024, SecureRandom.getInstanceStrong());

            KeyPair kp = kpg.generateKeyPair();

            String publicPem = toPem("PUBLIC KEY", kp.getPublic().getEncoded());
            String privatePem = toPem("PRIVATE KEY", kp.getPrivate().getEncoded()); // PKCS#8

            return new RsaKeys(publicPem, privatePem);
        } catch (Exception e) {
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    private String toPem(String type, byte[] derBytes) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }

    public record RsaKeys(String publicKeyPem, String privateKeyPem) {}
}
