package com.chatter.chatter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class MediaStreamResult {

    @NotNull
    private Resource resource;

    @NotNull
    private MediaType contentType;

    private long contentLength;

    private boolean isPartialContent;

}
