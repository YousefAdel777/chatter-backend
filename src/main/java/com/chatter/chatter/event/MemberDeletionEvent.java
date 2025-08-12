package com.chatter.chatter.event;

import com.chatter.chatter.model.Member;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MemberDeletionEvent {

    private Member member;

}
