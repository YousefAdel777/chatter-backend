package com.chatter.chatter.unit.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.VoteRepository;
import com.chatter.chatter.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private OptionService optionService;

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private VoteService voteService;

    @Test
    void createVotes_ShouldCreateVotes_WhenValidRequest() {
        User user = User.builder().email("user@example.com").build();
        PollMessage pollMessage = PollMessage.builder()
                .id(1L)
                .multiple(true)
                .endsAt(Instant.now().plusSeconds(3600))
                .build();
        Option option1 = Option.builder().id(1L).pollMessage(pollMessage).build();
        Option option2 = Option.builder().id(2L).pollMessage(pollMessage).build();
        List<Option> options = List.of(option1, option2);

        when(userService.getUserEntityByEmail("user@example.com")).thenReturn(user);
        when(optionService.getOptionsWithoutVotes("user@example.com", Set.of(1L, 2L))).thenReturn(options);
        when(voteRepository.existsByOptionPollMessageIdAndUserEmail(1L, "user@example.com")).thenReturn(false);
        when(voteRepository.saveAll(any())).thenReturn(List.of(new Vote(), new Vote()));

        List<Vote> result = voteService.createVotes("user@example.com", Set.of(1L, 2L));

        assertEquals(2, result.size());
        verify(voteRepository).saveAll(any());
        verify(messageService).broadcastMessageUpdate(pollMessage);
        verify(messageService).evictMessageCaches(pollMessage);
    }

    @Test
    void createVotes_ShouldThrowException_WhenNoValidOptions() {
        when(optionService.getOptionsWithoutVotes("user@example.com", Set.of(1L, 2L))).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> {
            voteService.createVotes("user@example.com", Set.of(1L, 2L));
        });
    }

    @Test
    void createVotes_ShouldThrowException_WhenPollEnded() {
        PollMessage pollMessage = PollMessage.builder()
                .id(1L)
                .endsAt(Instant.now().minusSeconds(3600))
                .build();
        Option option = Option.builder().id(1L).pollMessage(pollMessage).build();

        when(optionService.getOptionsWithoutVotes("user@example.com", Set.of(1L))).thenReturn(List.of(option));

        assertThrows(BadRequestException.class, () -> {
            voteService.createVotes("user@example.com", Set.of(1L));
        });
    }

    @Test
    void createVotes_ShouldThrowException_WhenOptionsFromDifferentPolls() {
        PollMessage pollMessage1 = PollMessage.builder().id(1L).endsAt(Instant.now().plusSeconds(3600)).build();
        PollMessage pollMessage2 = PollMessage.builder().id(2L).endsAt(Instant.now().plusSeconds(3600)).build();
        Option option1 = Option.builder().id(1L).pollMessage(pollMessage1).build();
        Option option2 = Option.builder().id(2L).pollMessage(pollMessage2).build();

        when(optionService.getOptionsWithoutVotes("user@example.com", Set.of(1L, 2L))).thenReturn(List.of(option1, option2));

        assertThrows(BadRequestException.class, () -> voteService.createVotes("user@example.com", Set.of(1L, 2L)));
    }

    @Test
    void createVotes_ShouldThrowException_WhenSinglePollAndMultipleOptions() {
        PollMessage pollMessage = PollMessage.builder()
                .id(1L)
                .multiple(false)
                .endsAt(Instant.now().plusSeconds(3600))
                .build();
        Option option1 = Option.builder().id(1L).pollMessage(pollMessage).build();
        Option option2 = Option.builder().id(2L).pollMessage(pollMessage).build();

        when(optionService.getOptionsWithoutVotes("user@example.com", Set.of(1L, 2L))).thenReturn(List.of(option1, option2));
        when(voteRepository.existsByOptionPollMessageIdAndUserEmail(1L, "user@example.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> voteService.createVotes("user@example.com", Set.of(1L, 2L)));
    }

    @Test
    void createVotes_ShouldThrowException_WhenSinglePollAndAlreadyVoted() {
        PollMessage pollMessage = PollMessage.builder()
                .id(1L)
                .multiple(false)
                .endsAt(Instant.now().plusSeconds(3600))
                .build();
        Option option = Option.builder().id(1L).pollMessage(pollMessage).build();

        when(optionService.getOptionsWithoutVotes("user@example.com", Set.of(1L))).thenReturn(List.of(option));
        when(voteRepository.existsByOptionPollMessageIdAndUserEmail(1L, "user@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> voteService.createVotes("user@example.com", Set.of(1L)));
    }

    @Test
    void deleteVotesByMessage_ShouldDeleteVotes_WhenValidRequest() {
        PollMessage pollMessage = PollMessage.builder()
                .id(1L)
                .messageType(MessageType.POLL)
                .endsAt(Instant.now().plusSeconds(3600))
                .build();

        when(messageService.getMessageEntity("user@example.com", 1L)).thenReturn(pollMessage);
        doNothing().when(voteRepository).deleteByOptionPollMessageIdAndUserEmail(1L, "user@example.com");

        voteService.deleteVotesByMessage("user@example.com", 1L);

        verify(voteRepository).deleteByOptionPollMessageIdAndUserEmail(1L, "user@example.com");
        verify(voteRepository).flush();
        verify(messageService).broadcastMessageUpdate(pollMessage);
        verify(messageService).evictMessageCaches(pollMessage);
    }

    @Test
    void deleteVotesByMessage_ShouldThrowException_WhenNotPollMessage() {
        Message message = Message.builder().messageType(MessageType.TEXT).build();

        when(messageService.getMessageEntity("user@example.com", 1L)).thenReturn(message);

        assertThrows(BadRequestException.class, () -> voteService.deleteVotesByMessage("user@example.com", 1L));
    }

    @Test
    void deleteVotesByMessage_ShouldThrowException_WhenPollEnded() {
        PollMessage pollMessage = PollMessage.builder()
                .id(1L)
                .messageType(MessageType.POLL)
                .endsAt(Instant.now().minusSeconds(3600))
                .build();

        when(messageService.getMessageEntity("user@example.com", 1L)).thenReturn(pollMessage);

        assertThrows(BadRequestException.class, () -> voteService.deleteVotesByMessage("user@example.com", 1L));
    }
}