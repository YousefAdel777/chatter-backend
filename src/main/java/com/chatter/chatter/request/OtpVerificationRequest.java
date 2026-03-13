package com.chatter.chatter.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OtpVerificationRequest {

    @NotBlank(message = "otp is required")
    private String otp;

    @Email
    @NotBlank(message = "email is required")
    private String email;

}
