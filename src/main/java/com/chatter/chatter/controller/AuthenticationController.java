package com.chatter.chatter.controller;

import com.chatter.chatter.dto.*;
import com.chatter.chatter.model.UserPrincipal;
import com.chatter.chatter.request.RefreshTokenRequest;
import com.chatter.chatter.request.UserLoginRequest;
import com.chatter.chatter.request.UserVerificationRequest;
import com.chatter.chatter.service.AuthenticationService;
import com.chatter.chatter.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@Valid @RequestBody UserLoginRequest userLoginDto) {
        return ResponseEntity.ok(authenticationService.login(userLoginDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenDto> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenDto) {
        TokenDto tokenDto = authenticationService.refreshToken(refreshTokenDto);
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody @Valid RefreshTokenRequest refreshTokenRequest,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authenticationService.logout(refreshTokenRequest, principal.getUser().getEmail());
        return ResponseEntity.ok(Map.of("message", "User logged out successfully"));
    }

    @GetMapping("/exchange-code")
    public ResponseEntity<TokenDto> exchangeCode(@RequestParam String code) {
        TokenDto tokenDto = jwtService.getTokensByCode(code);
        if (tokenDto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/verification")
    public ResponseEntity<TokenDto> verifyUser(
            @Valid @RequestBody UserVerificationRequest request
    ) {
        TokenDto tokenDto = authenticationService.verifyUser(request.getToken());
        return ResponseEntity.ok(tokenDto);
    }

}
