package uz.raqamli_talim.oneedu.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final String secret;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    public String generateJWTToken(UserDetailsImpl user) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusMillis(SecurityConstant.TOKEN_EXPIRE_AT))
                .sign(Algorithm.HMAC256(secret));
    }

    public String getUsernameFromToken(String jwtToken) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(jwtToken)
                .getSubject();
    }

    public boolean validateToken(String jwtToken) {
        try {
            JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(jwtToken);
            return true;
        } catch (JWTVerificationException ex) {
            log.error("JWT token validatsiyada xatolik", ex);
            return false;
        }
    }

    public long getExpirationTime(String jwtToken) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(jwtToken);

            Date expiresAt = decodedJWT.getExpiresAt();
            return expiresAt.getTime() - System.currentTimeMillis();
        } catch (JWTVerificationException ex) {
            log.error("Tokenni expiration vaqtini olishda xatolik", ex);
            return 0;
        }
    }
}
