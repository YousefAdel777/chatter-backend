package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserDto {

    @NotNull
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String email;

    @NotNull
    private Instant createdAt;

    @NotBlank
    private String image;

    private String bio;

    private Instant lastOnline;

    @Builder.Default
    private Boolean showOnlineStatus = true;

    @Builder.Default
    private Boolean showMessageReads = true;
}
