package com.chatter.chatter.request;

import com.chatter.chatter.model.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SingleMessageRequest extends BaseMessageRequest {

    private Long userId;

    private Long chatId;

    private Boolean isEveryoneMentioned;

    private Set<Long> mentionedUsersIds;

}
