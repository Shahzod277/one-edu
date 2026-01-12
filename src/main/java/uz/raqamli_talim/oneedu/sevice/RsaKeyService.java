package uz.raqamli_talim.oneedu.sevice;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class RsaKeyService {

    private static final SecureRandom RNG = new SecureRandom();

    // API KEY
    public String generateApiKey() {
        byte[] bytes = new byte[32]; // 256-bit
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // RSA KEY PAIR (Base64 DER, PEM emas)
    public RsaKeys generateRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, SecureRandom.getInstanceStrong());

            KeyPair kp = kpg.generateKeyPair();

            String publicKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            String privateKeyBase64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

            return new RsaKeys(publicKeyBase64, privateKeyBase64);
        } catch (Exception e) {
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    public record RsaKeys(String publicKey, String privateKey) {}

    // ✅ Utils.encode() bilan bir xil: RSA (PKCS1Padding) + oddiy Base64
    public  String encrypt(String publicKeyBase64, String message) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Cipher cipher = Cipher.getInstance("RSA"); // => RSA/ECB/PKCS1Padding
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // ✅ oddiy Base64 (URL-safe emas) — Utils.encode() bilan MOS
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt failed", e);
        }
    }


}
