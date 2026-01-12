package uz.raqamli_talim.oneedu.sevice;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
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

    // RSA KEY PAIR (PEM YO‘Q!)
    public RsaKeys generateRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

            // ❗ tavsiya: 2048
            kpg.initialize(2048, SecureRandom.getInstanceStrong());

            KeyPair kp = kpg.generateKeyPair();

            String publicKeyBase64 =
                    Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

            String privateKeyBase64 =
                    Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

            return new RsaKeys(publicKeyBase64, privateKeyBase64);

        } catch (Exception e) {
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    public record RsaKeys(String publicKey, String privateKey) {
    }


    public static String encrypt(String publicKey, String message) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);

            PublicKey publicKeyObj = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKeyObj);

            byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // JSON body uchun ok:
            return Base64.getEncoder().encodeToString(encryptedBytes);

            // Redirect URL uchun yaxshiroq:
            // return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedBytes);

        } catch (Exception e) {
            throw new IllegalStateException("RSA encrypt failed", e);
        }
    }


}


