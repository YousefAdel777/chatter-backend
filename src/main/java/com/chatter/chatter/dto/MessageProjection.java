package com.chatter.chatter.dto;

import com.chatter.chatter.model.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageProjection {

    private Message message;

    private Boolean isStarred;

    private Boolean isSeen;

}