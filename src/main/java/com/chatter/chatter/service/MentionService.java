package com.chatter.chatter.service;

import com.chatter.chatter.model.Mention;
import com.chatter.chatter.model.Message;
import com.chatter.chatter.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MentionService {

    private final UserService userService;

    public Set<Mention> createMentions(Message message, Set<Long> usersIds) {
        List<User> users = userService.getUserEntitiesByChatMembership(message.getChat().getId(), usersIds);
        Set<Mention> mentions = new HashSet<>();
        for (User user : users) {
            Mention mention = Mention.builder()
                    .user(user)
                    .message(message)
                    .build();
            mentions.add(mention);
        }
        return mentions;
    }

}
