package com.chatter.chatter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessagePatchRequest {
    @NotBlank(message = "content is required")
    private String  content;

    @NotBlank(message = "contentJson is required")
    private String contentJson;

}