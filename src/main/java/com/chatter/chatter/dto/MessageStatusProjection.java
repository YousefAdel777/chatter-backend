package com.chatter.chatter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageStatusProjection {

    private Long id;

    private Long userId;

    private String email;

    private Boolean isSeen;

    private Boolean isStarred;

}
