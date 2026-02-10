package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.LoginRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.sevice.AuthService;
import uz.raqamli_talim.oneedu.sevice.ClientSystemService;
import uz.raqamli_talim.oneedu.sevice.EmployeeHemisService;
import uz.raqamli_talim.oneedu.sevice.MyHemisService;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ClientSystemService clientSystemService;
    private final MyHemisService myHemisService;
    private final EmployeeHemisService employeeHemisService;

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
            @RequestParam("state") String state
    ) {

        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state bo‘sh kelib qoldi");
        }

        String system;
        String type;
        String universityCode = null;

        // my-employee|380
        if (state.contains("|")) {
            String[] parts = state.split("\\|", 2);
            system = parts[0];
            universityCode = parts[1];
        } else {
            system = state;
        }

        URI redirectUri;

        if ("my-hemis".equalsIgnoreCase(system)) {

            redirectUri =
                    myHemisService.oneIdAdminSignInAndRedirect(code, state);

        } else if ("my-tutor".equalsIgnoreCase(system)) {
            type = "tutor";
            redirectUri =
                    employeeHemisService.oneIdAdminSignInAndRedirect(
                            code, state,
                            universityCode ,type
                    );

        } else {

            redirectUri =
                    authService.oneIdAdminSignInAndRedirect(code, state);
        }

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

    @GetMapping("my-tutor")
    public ResponseEntity<Void> getOneIdAdminEmployee(@RequestParam("universityCode") String universityCode
    ) {
        String apiKey = "my-tutor" + universityCode;
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
