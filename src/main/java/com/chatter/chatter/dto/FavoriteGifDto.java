package com.chatter.chatter.dto;

import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FavoriteGifDto {

    private Long id;

    private String gifId;

    private Long userId;

    private Instant createdAt;

}
