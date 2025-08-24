package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.BlockDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.BlockMapper;
import com.chatter.chatter.model.Block;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.BlockRepository;
import com.chatter.chatter.service.BlockService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class BlockServiceTests {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserService userService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private BlockMapper blockMapper;

    @InjectMocks
    private BlockService blockService;

    private final User user1 = User.builder()
            .id(1L)
            .email("testEmail1@example.com")
            .password("testPassword1")
            .username("testUsername1")
            .build();

    private final User user2 = User.builder()
            .id(2L)
            .email("testEmail2@example.com")
            .password("testPassword2")
            .username("testUsername2")
            .build();

    private Block block = Block.builder()
            .id(1L)
            .blockedBy(user1)
            .blockedUser(user2)
            .build();

    @Test
    void getBlockEntity_ShouldReturnEntity_WhenFound() {
        when(blockRepository.findByIdAndBlockedByEmail(block.getId(), user1.getEmail())).thenReturn(Optional.of(block));

        Block result = blockService.getBlockEntity(user1.getEmail(), block.getId());

        verify(blockRepository).findByIdAndBlockedByEmail(block.getId(), user1.getEmail());
        assertEquals(block.getId(), result.getId());
        assertEquals(block.getBlockedUser().getId(), result.getBlockedUser().getId());
        assertEquals(block.getBlockedBy().getId(), result.getBlockedBy().getId());
    }

    @Test
    void getBlockEntity_ShouldThrow_WhenNotFound() {
        when(blockRepository.findByIdAndBlockedByEmail(block.getId(), user1.getEmail())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> blockService.getBlockEntity(user1.getEmail(), block.getId()));
    }

    @Test
    void getBlock_ShouldReturnDto_WhenFound() {
        BlockDto dto = BlockDto.builder()
                .id(block.getId())
                .blockedBy(null)
                .blockedUser(null)
                .build();

        when(blockRepository.findByIdAndBlockedByEmail(block.getId(), user1.getEmail())).thenReturn(Optional.of(block));
        when(blockMapper.toDto(block)).thenReturn(dto);

        BlockDto result = blockService.getBlock(user1.getEmail(), block.getId());
        verify(blockRepository).findByIdAndBlockedByEmail(block.getId(), user1.getEmail());
        assertEquals(dto.getId(), result.getId());
        assertNull(result.getBlockedBy());
        assertNull(result.getBlockedUser());
    }

    @Test
    void getBlock_ShouldThrow_WhenNotFound() {
        when(blockRepository.findByIdAndBlockedByEmail(block.getId(), user1.getEmail())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> blockService.getBlock(user1.getEmail(), block.getId()));
    }

    @Test
    void getUserBlockEntity_ShouldReturnEntity_WhenFound() {

        when(blockRepository.findByBlockedByEmailAndBlockedUserId(user1.getEmail(), block.getBlockedUser().getId())).thenReturn(Optional.of(block));

        Block result = blockService.getUserBlockEntity(user1.getEmail(), block.getBlockedUser().getId());

        verify(blockRepository).findByBlockedByEmailAndBlockedUserId(user1.getEmail(), block.getBlockedUser().getId());
        assertEquals(block.getId(), result.getId());
        assertEquals(block.getBlockedBy().getId(), result.getBlockedBy().getId());
        assertEquals(block.getBlockedUser().getId(), result.getBlockedUser().getId());
    }

    @Test
    void getUserBlockEntity_ShouldThrow_WhenNotFound() {
        when(blockRepository.findByBlockedByEmailAndBlockedUserId(user1.getEmail(), block.getBlockedUser().getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> blockService.getUserBlockEntity(user1.getEmail(), block.getBlockedUser().getId()));
    }

    @Test
    void getUserBlock_ShouldReturnDto_WhenFound() {
        BlockDto dto = BlockDto.builder()
                .id(block.getId())
                .blockedBy(null)
                .blockedUser(null)
                .build();

        when(blockRepository.findByBlockedByEmailAndBlockedUserId(user1.getEmail(), block.getBlockedUser().getId())).thenReturn(Optional.of(block));
        when(blockMapper.toDto(block)).thenReturn(dto);

        BlockDto result = blockService.getUserBlock(user1.getEmail(), block.getBlockedUser().getId());

        verify(blockRepository).findByBlockedByEmailAndBlockedUserId(user1.getEmail(), block.getBlockedUser().getId());
        assertEquals(dto.getId(), result.getId());
        assertNull(result.getBlockedBy());
        assertNull(result.getBlockedUser());
    }

    @Test
    void getUserBlock_ShouldThrow_WhenNotFound() {
        when(blockRepository.findByBlockedByEmailAndBlockedUserId(user1.getEmail(), block.getBlockedUser().getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> blockService.getUserBlock(user1.getEmail(), block.getBlockedUser().getId()));
    }

    @Test
    void isBlocked_ReturnsTrue_WhenUserCreatesBlock() {
        when(blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId())).thenReturn(true);
        boolean result =  blockService.isBlocked(user1.getEmail(), block.getBlockedUser().getId());
        verify(blockRepository).existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId());
        verify(blockRepository, never()).existsByBlockedByIdAndBlockedUserEmail(user2.getId(), user1.getEmail());
        assertTrue(result);
    }

    @Test
    void isBlocked_ReturnsTrue_WhenUserIsBlocked() {
        when(blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId())).thenReturn(false);
        when(blockRepository.existsByBlockedByIdAndBlockedUserEmail(user2.getId(), user1.getEmail())).thenReturn(true);
        boolean result =  blockService.isBlocked(user1.getEmail(), block.getBlockedUser().getId());
        verify(blockRepository).existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId());
        verify(blockRepository).existsByBlockedByIdAndBlockedUserEmail(user2.getId(), user1.getEmail());
        assertTrue(result);
    }

    @Test
    void isBlocked_ReturnsFalse_WhenBlockNotExistsBetweenUsers() {
        when(blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId())).thenReturn(false);
        when(blockRepository.existsByBlockedByIdAndBlockedUserEmail(user2.getId(), user1.getEmail())).thenReturn(false);
        boolean result =  blockService.isBlocked(user1.getEmail(), block.getBlockedUser().getId());
        verify(blockRepository).existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId());
        verify(blockRepository).existsByBlockedByIdAndBlockedUserEmail(user2.getId(), user1.getEmail());
        assertFalse(result);
    }

    @Test
    void getUserBlockEntities_ShouldReturnListOfAllBlocksCreatedByUser() {
        when(blockRepository.findAllByBlockedByEmail(user1.getEmail())).thenReturn(List.of(block));
        List<Block> blocks = blockService.getUserBlockEntities(user1.getEmail());
        verify(blockRepository).findAllByBlockedByEmail(user1.getEmail());
        assertEquals(1, blocks.size());
        assertEquals(block.getId(), blocks.getFirst().getId());
    }

    @Test
    void getUserBlocks_ShouldReturnListOfAllBlockDtosCreatedByUser() {
        BlockDto dto = BlockDto.builder()
                .id(block.getId())
                .blockedBy(null)
                .blockedUser(null)
                .build();

        when(blockRepository.findAllByBlockedByEmail(user1.getEmail())).thenReturn(List.of(block));
        when(blockMapper.toDtoList(List.of(block))).thenReturn(List.of(dto));

        List<BlockDto> blockDtos = blockService.getUserBlocks(user1.getEmail());

        verify(blockRepository).findAllByBlockedByEmail(user1.getEmail());
        verify(blockMapper).toDtoList(List.of(block));
        assertEquals(1, blockDtos.size());
        assertEquals(dto.getId(), blockDtos.getFirst().getId());
        assertNull(blockDtos.getFirst().getBlockedBy());
        assertNull(blockDtos.getFirst().getBlockedUser());
    }

    @Test
    void createBlock_ShouldThrow_WhenBlockExists() {
        when(blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId())).thenReturn(true);
        assertThrows(BadRequestException.class, () -> blockService.createBlock(user1.getEmail(), user2.getId()));
    }

    @Test
    void createBlock_ShouldThrow_WhenSelfBlock() {
        when(blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user1.getId())).thenReturn(false);
        when(userService.getUserEntity(user1.getId())).thenReturn(user1);
        when(userService.getUserEntityByEmail(user1.getEmail())).thenReturn(user1);
        assertThrows(BadRequestException.class, () -> blockService.createBlock(user1.getEmail(), user1.getId()));
    }

    @Test
    void createBlock_ShouldCreateNewBlock_WhenBlockNotExistsAndNotSelfBlock() {
        when(blockRepository.existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId())).thenReturn(false);
        when(userService.getUserEntity(user2.getId())).thenReturn(user2);
        when(userService.getUserEntityByEmail(user1.getEmail())).thenReturn(user1);
        when(blockRepository.save(any(Block.class))).thenAnswer(i -> i.getArgument(0));
        Block result = blockService.createBlock(user1.getEmail(), user2.getId());

        verify(blockRepository).save(any(Block.class));
        verify(blockRepository).existsByBlockedByEmailAndBlockedUserId(user1.getEmail(), user2.getId());
        verify(userService).getUserEntityByEmail(user1.getEmail());
        verify(userService).getUserEntity(user2.getId());

        assertNull(result.getId());
        assertEquals(user1.getId(), result.getBlockedBy().getId());
        assertEquals(user2.getId(), result.getBlockedUser().getId());
    }

    @Test
    void deleteBlock_ShouldThrow_WhenBlockNotExists() {
        when(blockRepository.findByIdAndBlockedByEmail(block.getId(), user1.getEmail())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> blockService.deleteBlock(user1.getEmail(), block.getId()));
    }

    @Test
    void deleteBlock_ShouldDeleteBlockAndEvictCaches_WhenBlockExists() {
        Cache blocksCache = mock(Cache.class);
        Cache isBlockedCache = mock(Cache.class);
        when(blockRepository.findByIdAndBlockedByEmail(block.getId(), user1.getEmail())).thenReturn(Optional.of(block));
        when(cacheManager.getCache("blocks")).thenReturn(blocksCache);
        when(cacheManager.getCache("isBlocked")).thenReturn(isBlockedCache);

        blockService.deleteBlock(user1.getEmail(), block.getId());

        verify(blockRepository).delete(any(Block.class));
        verify(blocksCache).evict("id:" + block.getId() + ":email:" + user1.getEmail());
        verify(blocksCache).evict("email:" + user1.getEmail());
        verify(blocksCache).evict("userId:" + block.getBlockedUser().getId() + ":email:" + user1.getEmail());
        isBlockedCache.evict("userId:" + block.getBlockedUser().getId() + ":email:" + user1.getEmail());
    }

}
