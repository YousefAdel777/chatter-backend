package com.chatter.chatter.request;

import lombok.*;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StoryPatchRequest {

    private Set<Long> excludedUsersIds;

}
