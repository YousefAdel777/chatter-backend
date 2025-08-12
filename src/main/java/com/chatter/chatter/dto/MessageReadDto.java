package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.mapping.UserDefinedType;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MessageReadDto {

    @NotNull
    private Long id;

    @NotNull
    private Long messageId;

    @NotNull
    private UserDto user;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Boolean showRead;

}
