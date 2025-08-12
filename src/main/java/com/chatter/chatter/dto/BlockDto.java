package com.chatter.chatter.dto;

import com.chatter.chatter.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BlockDto {

    @NotNull
    private Long id;

    @NotNull
    private UserDto blockedBy;

    @NotNull
    private UserDto blockedUser;

}
