package com.chatter.chatter.dto;

import com.chatter.chatter.model.MemberRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MemberPatchRequest {

    private MemberRole memberRole;

}
