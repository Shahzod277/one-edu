package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.LoginRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.sevice.AuthService;
import uz.raqamli_talim.oneedu.sevice.ClientSystemService;
import uz.raqamli_talim.oneedu.sevice.MyHemisService;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ClientSystemService clientSystemService;
    private final MyHemisService myHemisService;

    @GetMapping("/{apiKey}")
    public ResponseEntity<Void> getOneIdAdmin(@PathVariable String apiKey) {
        URI uri = authService.redirectOneIdUrlAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> oneIdAdminSignIn(
            @RequestParam("code") String code,
            @RequestParam("state") String state // apiKey
    ) {

        URI redirectUri =
                "my-hemis".equalsIgnoreCase(state)
                        ? myHemisService.oneIdAdminSignInAndRedirect(code, state)
                        : authService.oneIdAdminSignInAndRedirect(code, state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @PostMapping("public/signIn")
    public ResponseEntity<?> signIn(@RequestBody LoginRequest request) {
        ResponseDto response = authService.signIn(request);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getCode()));
    }

    @GetMapping("my-hemis")
    public ResponseEntity<Void> getOneIdAdmin() {
        String apiKey = "my-hemis";
        URI uri = authService.redirectOneIdUrlAdmin(apiKey);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }
//    @GetMapping("my-hemis")
//    public ResponseEntity<Void> getOneIdAdmin() {
//        String apiKey = "my-hemis";
//        URI uri = authService.redirectOneIdUrlAdmin(apiKey);
//        return ResponseEntity.status(HttpStatus.FOUND)
//                .location(uri)
//                .build();
//    }

}
