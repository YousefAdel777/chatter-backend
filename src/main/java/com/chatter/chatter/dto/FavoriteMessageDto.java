package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class FavoriteMessageDto {

    @NotNull
    private Long id;

    @NotNull
    private MessageDto messageDto;

    @NotNull
    private Long userId;

}
