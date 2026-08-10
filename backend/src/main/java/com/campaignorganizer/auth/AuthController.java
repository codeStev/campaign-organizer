package com.campaignorganizer.auth;

import com.campaignorganizer.config.AppProperties;
import com.campaignorganizer.security.JwtService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppProperties properties;
    private final JwtService jwtService;

    public AuthController(AppProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        if (!matchesConfiguredPassword(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }
        JwtService.IssuedToken issued = jwtService.issue();
        return TokenResponse.bearer(issued.token(), issued.expiresAt());
    }

    /** Constant-time comparison to avoid leaking password length/prefix via timing. */
    private boolean matchesConfiguredPassword(String candidate) {
        byte[] expected = properties.password().getBytes(StandardCharsets.UTF_8);
        byte[] actual = candidate.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
