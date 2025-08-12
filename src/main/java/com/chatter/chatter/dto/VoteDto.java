package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class VoteDto {

    @NotNull
    private Long id;

    @NotNull
    private UserDto user;

    @NotNull
    private Long optionId;

}
