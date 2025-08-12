package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VotePostRequest {

    @NotNull(message = "optionsIds is required")
    @Size(min = 1, message = "optionsIds cannot be empty")
    private Set<Long> optionsIds;
}
