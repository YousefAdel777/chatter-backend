package com.chatter.chatter.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OtpEmailEvent {

    @Email
    private String email;

    @NotBlank(message = "otp is required")
    private String otp;

    @NotBlank(message = "username is required")
    private String username;

}
