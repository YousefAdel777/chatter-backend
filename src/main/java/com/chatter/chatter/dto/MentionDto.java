package com.chatter.chatter.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MentionDto {

    private Long id;

    private UserDto user;

}
