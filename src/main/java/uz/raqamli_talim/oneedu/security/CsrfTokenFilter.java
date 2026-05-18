package uz.raqamli_talim.oneedu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CsrfTokenFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.equals("/one-id-login.html")) {
            String token = generateToken();
            Cookie cookie = new Cookie(CSRF_COOKIE, token);
            cookie.setPath("/");
            cookie.setHttpOnly(false);
            cookie.setSecure(request.isSecure());
            cookie.setMaxAge(600);
            response.addCookie(cookie);
            filterChain.doFilter(request, response);
            return;
        }

        if (uri.equals("/api/auth/direct-login") && "POST".equalsIgnoreCase(request.getMethod())) {
            String cookieToken = getCookieValue(request, CSRF_COOKIE);
            String headerToken = request.getHeader(CSRF_HEADER);

            if (cookieToken == null || cookieToken.isBlank()
                    || headerToken == null || !cookieToken.equals(headerToken)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"code\":403,\"message\":\"CSRF token noto'g'ri yoki mavjud emas\",\"success\":false}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
