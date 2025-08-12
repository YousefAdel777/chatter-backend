package com.chatter.chatter.event;

import com.chatter.chatter.model.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MemberJoinEvent {

    private Member member;

}
