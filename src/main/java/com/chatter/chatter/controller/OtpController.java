package com.chatter.chatter.controller;

import com.chatter.chatter.request.OtpSendRequest;
import com.chatter.chatter.request.OtpVerificationRequest;
import com.chatter.chatter.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/verification")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request
    ) {
        String token = otpService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Void> sendOtp(
            @Valid @RequestBody OtpSendRequest request
    ) {
        otpService.generateOtpAndSend(request.getEmail());
        return ResponseEntity.ok().build();
    }

}
