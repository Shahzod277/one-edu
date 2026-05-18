package uz.raqamli_talim.oneedu.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PkceSessionStore {

    private final Map<String, PkceSession> sessions = new ConcurrentHashMap<>();

    private static final long TTL_SECONDS = 300; // 5 minut

    public record PkceSession(
            String codeChallenge,
            String state,
            String redirectUri,
            HemisTokenResult tokenResult,
            Instant createdAt
    ) {
        public PkceSession withTokenResult(HemisTokenResult result) {
            return new PkceSession(codeChallenge, state, redirectUri, result, createdAt);
        }
    }

    public record HemisTokenResult(
            String token,
            String refreshToken,
            String apiUrl,
            String error
    ) {}

    public void save(String sessionId, String codeChallenge, String state, String redirectUri) {
        sessions.put(sessionId, new PkceSession(codeChallenge, state, redirectUri, null, Instant.now()));
    }

    public PkceSession get(String sessionId) {
        PkceSession session = sessions.get(sessionId);
        if (session == null) return null;
        if (isExpired(session)) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    public void updateWithResult(String sessionId, HemisTokenResult result) {
        PkceSession session = sessions.get(sessionId);
        if (session != null) {
            sessions.put(sessionId, session.withTokenResult(result));
        }
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        sessions.entrySet().removeIf(e -> isExpired(e.getValue()));
    }

    private boolean isExpired(PkceSession session) {
        return Instant.now().isAfter(session.createdAt().plusSeconds(TTL_SECONDS));
    }
}
