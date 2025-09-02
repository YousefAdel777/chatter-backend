package com.chatter.chatter.dto;

import com.azure.core.annotation.Get;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageStatusProjection {

    private Long id;

    private Boolean isSeen;

    private Boolean isStarred;

}
