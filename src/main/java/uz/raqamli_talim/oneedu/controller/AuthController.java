package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.sevice.AuthService;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("oneIdAdmin/{apiKey}")
    public ResponseEntity<?> getOneIdAdmin(@PathVariable String apiKey) {
        URI uri = authService.redirectOneIdUrlAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }


    @GetMapping("auth")
    public ResponseEntity<?> oneIdAdminSignIn(
            @RequestParam("code") String code,
            @RequestParam("state") String state   // ⬅️ BU apiKey
    ) {
        // service hamma ishni qiladi
        URI redirectUri = authService.oneIdAdminSignInAndRedirect(code, state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }
}
