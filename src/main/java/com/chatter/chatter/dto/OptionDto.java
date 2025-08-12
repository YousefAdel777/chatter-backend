package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OptionDto {

    @NotNull
    private Long id;

    @NotNull
    @NotBlank
    private String title;

    @NotNull
    private Long messageId;

    @Builder.Default
    private List<VoteDto> votes = new ArrayList<>();

}
