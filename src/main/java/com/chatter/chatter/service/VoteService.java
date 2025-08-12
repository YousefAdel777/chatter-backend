package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.model.*;
import com.chatter.chatter.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final OptionService optionService;
    private final MessageService messageService;
    private final UserService userService;

    @Transactional
    public List<Vote> createVotes(String email, Set<Long> optionsIds) {
        List<Vote> votes = new ArrayList<>();
        User user = userService.getUserEntityByEmail(email);
        List<Option> options = optionService.getOptions(email, optionsIds);
        if (options.isEmpty()) {
            throw new BadRequestException("options", "No valid options selected");
        }
        PollMessage pollMessage = options.getFirst().getPollMessage();
        if (pollMessage.ended()) {
            throw new BadRequestException("poll", "poll has ended");
        }
        Set<Long> pollIds = options.stream()
                .map(opt -> opt.getPollMessage().getId())
                .collect(Collectors.toSet());
        if (pollIds.size() > 1) {
            throw new BadRequestException("options", "All options must belong to the same poll");
        }
        boolean hasVoted = voteRepository.existsByOptionPollMessageIdAndUserEmail(pollMessage.getId(), email);
        if (!pollMessage.getMultiple() && (optionsIds.size() > 1 || hasVoted)) {
            throw new BadRequestException("message", "Only one option is allowed");
        }
        for (Option option : options) {
            Vote vote = Vote.builder()
                    .user(user)
                    .option(option)
                    .build();
            votes.add(vote);
            option.addVote(vote);
        }
        voteRepository.saveAll(votes);
        messageService.broadcastMessageUpdate(pollMessage);
        messageService.evictMessageCaches(pollMessage);
        return votes;
    }

    @Transactional
    public void deleteVotesByMessage(String email, Long messageId) {
        Message message = messageService.getMessageEntity(email, messageId);
        if (!message.getMessageType().equals(MessageType.POLL)) {
            throw new BadRequestException("message", "message is not a poll");
        }
        PollMessage pollMessage = (PollMessage) message;
        if (pollMessage.ended()) {
            throw new BadRequestException("poll", "Cannot modify votes for ended poll");
        }
        voteRepository.deleteByOptionPollMessageIdAndUserEmail(messageId, email);
        voteRepository.flush();
        messageService.broadcastMessageUpdate(message);
        messageService.evictMessageCaches(message);
    }

}
