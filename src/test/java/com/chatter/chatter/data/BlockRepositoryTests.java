package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.Block;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.BlockRepository;
import com.chatter.chatter.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class BlockRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockRepository blockRepository;

    private User user1;
    private User user2;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        blockRepository.deleteAll();

        User u1 = User.builder()
                .email("testId1@example.com")
                .username("testUsername1")
                .password("testPassword1")
                .build();
        user1 = userRepository.save(u1);

        User u2 = User.builder()
                .email("testId2@example.com")
                .username("testUsername1")
                .password("testPassword2")
                .build();
        user2 = userRepository.save(u2);
    }

    @Test
    @Transactional
    void shouldSaveBlock() {

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        Block createdBlock = blockRepository.save(block);
        assertNotNull(createdBlock);
        assertNotNull(createdBlock.getId());
        assertEquals(user1.getId(), createdBlock.getBlockedBy().getId());
        assertEquals(user2.getId(), createdBlock.getBlockedUser().getId());
    }

    @Test
    @Transactional
    void shouldThrowIfBlockExistsBetweenUsers() {

        Block block1 = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        blockRepository.save(block1);
        Block block2 = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> blockRepository.save(block2));
    }

    @Test
    @Transactional
    void shouldFindAllBlocksUserCreated() {

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        blockRepository.save(block);

        List<Block> blocks =  blockRepository.findAllByBlockedById(user1.getId());
        assertEquals(1, blocks.size());
        assertEquals(user1.getId(), blocks.getFirst().getBlockedBy().getId());
        assertEquals(user2.getId(), blocks.getFirst().getBlockedUser().getId());

    }

    @Test
    @Transactional
    void shouldFindBlockByUserIdAndBlockedUserId() {

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        blockRepository.save(block);

        Block foundBlock =  blockRepository.findByBlockedByIdAndBlockedUserId(user1.getId(), user2.getId()).orElse(null);
        assertNotNull(foundBlock);
        assertEquals(user1.getId(), foundBlock.getBlockedBy().getId());
        assertEquals(user2.getId(), foundBlock.getBlockedUser().getId());
    }

    @Test
    @Transactional
    void shouldNotFindBlockByUserIdAndBlockedByUserId() {
        Block foundBlock =  blockRepository.findByBlockedByIdAndBlockedUserId(user1.getId(), user2.getId()).orElse(null);
        assertNull(foundBlock);
    }

    @Test
    @Transactional
    void shouldFindBlockByIdAndBlockedByIdIfExists() {

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        Block createdBlock = blockRepository.save(block);

        Block foundBlock =  blockRepository.findByIdAndBlockedById(createdBlock.getId(), user1.getId()).orElse(null);
        assertNotNull(foundBlock);
        assertEquals(user1.getId(), foundBlock.getBlockedBy().getId());
        assertEquals(user2.getId(), foundBlock.getBlockedUser().getId());

    }

    @Test
    @Transactional
    void shouldNotFindBlockByIdAndBlockedByIdIfNotExists() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        Block createdBlock = blockRepository.save(block);
        Block foundBlock =  blockRepository.findByIdAndBlockedById(createdBlock.getId(), user2.getId()).orElse(null);
        assertNull(foundBlock);
    }

    @Test
    @Transactional
    void shouldReturnTrueIfBlockExistsByBlockedByIdAndBlockedUserId() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);
        boolean exists =  blockRepository.existsByBlockedByIdAndBlockedUserId(user1.getId(), user2.getId());
        assertTrue(exists);
    }

    @Test
    @Transactional
    void shouldReturnFalseIfBlockNotExistsByBlockedByIdAndBlockedUserId() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);
        boolean exists =  blockRepository.existsByBlockedByIdAndBlockedUserId(user2.getId(), user1.getId());
        assertFalse(exists);
    }

}
