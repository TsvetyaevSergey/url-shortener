package dev.horoz.url_shortener.api.auth;

import dev.horoz.url_shortener.api.auth.dto.LoginRequest;
import dev.horoz.url_shortener.api.auth.dto.LoginResponse;
import dev.horoz.url_shortener.api.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.email(), req.password());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String token = authService.loginAndIssueToken(req.email(), req.password());
        return new LoginResponse(req.email(), token);
    }

}
