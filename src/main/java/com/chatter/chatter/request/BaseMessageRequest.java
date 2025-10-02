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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BaseMessageRequest {

    private String  content;

    private String contentJson;

    private Long replyMessageId;

    @NotNull(message = "messageType is required")
    private MessageType messageType;

    private List<MultipartFile> mediaFiles;

    private MultipartFile file;

    private Long duration;

    private Boolean missed;

    private List<String> options;

    private Instant endsAt;

    private Boolean multiple = false;

    private String title;

    private Long storyId;

    private Long inviteId;

}
