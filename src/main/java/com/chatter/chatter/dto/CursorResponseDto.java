package com.chatter.chatter.dto;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CursorResponseDto<T> {

    private List<T> content;

    private Long nextCursor;

    private Long previousCursor;

}
