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
                .email("testEmail1@example.com")
                .username("testUsername1")
                .password("testPassword1")
                .build();
        user1 = userRepository.save(u1);

        User u2 = User.builder()
                .email("testEmail2@example.com")
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

        List<Block> blocks =  blockRepository.findAllByBlockedByEmail(user1.getEmail());
        assertEquals(1, blocks.size());
        assertEquals(user1.getId(), blocks.getFirst().getBlockedBy().getId());
        assertEquals(user2.getId(), blocks.getFirst().getBlockedUser().getId());

    }

    @Test
    @Transactional
    void shouldFindBlockByUserEmailAndBlockedUserId() {

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        blockRepository.save(block);

        Block foundBlock =  blockRepository.findByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId()).orElse(null);
        assertNotNull(foundBlock);
        assertEquals(user1.getId(), foundBlock.getBlockedBy().getId());
        assertEquals(user2.getId(), foundBlock.getBlockedUser().getId());
    }

    @Test
    @Transactional
    void shouldNotFindBlockByUserEmailAndBlockedByUserId() {
        Block foundBlock =  blockRepository.findByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId()).orElse(null);
        assertNull(foundBlock);
    }

    @Test
    @Transactional
    void shouldFindBlockByIdAndBlockedByEmailIfExists() {

        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();

        Block createdBlock = blockRepository.save(block);

        Block foundBlock =  blockRepository.findByIdAndBlockedByEmail(createdBlock.getId(), user1.getEmail()).orElse(null);
        assertNotNull(foundBlock);
        assertEquals(user1.getId(), foundBlock.getBlockedBy().getId());
        assertEquals(user2.getId(), foundBlock.getBlockedUser().getId());

    }

    @Test
    @Transactional
    void shouldNotFindBlockByIdAndBlockedByEmailIfNotExists() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        Block createdBlock = blockRepository.save(block);
        Block foundBlock =  blockRepository.findByIdAndBlockedByEmail(createdBlock.getId(), user2.getEmail()).orElse(null);
        assertNull(foundBlock);
    }

    @Test
    @Transactional
    void shouldReturnTrueIfBlockExistsByBlockedByEmailAndBlockedUserId() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);
        boolean exists =  blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId());
        assertTrue(exists);
    }

    @Test
    @Transactional
    void shouldReturnFalseIfBlockNotExistsByBlockedByEmailAndBlockedUserId() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);
        boolean exists =  blockRepository.existsByBlockedByEmailAndBlockedUserId(user2.getEmail(), user1.getId());
        assertFalse(exists);
    }

    @Test
    @Transactional
    void shouldReturnTrueIfBlockExistsByBlockedByIdAndBlockedUserEmail() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);
        boolean exists =  blockRepository.existsByBlockedByIdAndBlockedUserEmail(user1.getId(), user2.getEmail());
        assertTrue(exists);
    }

    @Test
    @Transactional
    void shouldReturnFalseIfBlockNotExistsByBlockedByIdAndBlockedUserEmail() {
        Block block = Block.builder()
                .blockedBy(user1)
                .blockedUser(user2)
                .build();
        blockRepository.save(block);
        boolean exists =  blockRepository.existsByBlockedByIdAndBlockedUserEmail(user2.getId(), user1.getEmail());
        assertFalse(exists);
    }

}
