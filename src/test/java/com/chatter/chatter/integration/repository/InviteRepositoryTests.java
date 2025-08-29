package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.GroupChat;
import com.chatter.chatter.model.Invite;
import com.chatter.chatter.repository.ChatRepository;
import com.chatter.chatter.repository.InviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
public class InviteRepositoryTests {

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private ChatRepository chatRepository;

    private GroupChat groupChat;

    @BeforeEach
    public void setup() {
        groupChat = chatRepository.save(GroupChat.builder()
                    .name("test name")
                    .image("test.jpg")
                    .description("test description")
                    .build());
    }

    @Test
    void shouldSaveInvite_WhenValid() {
        Invite invite = Invite.builder()
                .groupChat(groupChat)
                .canUseLink(true)
                .build();

        Invite savedInvite = inviteRepository.save(invite);
        assertNotNull(savedInvite.getId());
        assertEquals(savedInvite.getGroupChat().getId(), groupChat.getId());
        assertNotNull(savedInvite.getCreatedAt());
        assertNull(savedInvite.getExpiresAt());
        assertTrue(savedInvite.getCanUseLink());
    }

}
