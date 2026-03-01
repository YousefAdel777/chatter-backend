package com.chatter.chatter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OtpVerificationRequest {

    @NotBlank(message = "otp is required")
    private String otp;

    @NotBlank(message = "token is required")
    private String email;

}
