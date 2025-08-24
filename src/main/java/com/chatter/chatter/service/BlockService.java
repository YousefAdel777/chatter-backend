package com.chatter.chatter.service;

import com.chatter.chatter.dto.BlockDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.BlockMapper;
import com.chatter.chatter.model.Block;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserService userService;
    private final CacheManager cacheManager;
    private final BlockMapper blockMapper;

    public Block getBlockEntity(String email, Long blockId) {
        return blockRepository.findByIdAndBlockedByEmail(blockId, email)
                .orElseThrow(() -> new NotFoundException("block", "not found"));
    }

    @Cacheable(value = "blocks", key = "'id:' + #blockId + ':email:' + #email")
    public BlockDto getBlock(String email, Long blockId) {
        return blockMapper.toDto(getBlockEntity(email, blockId));
    }

    public Block getUserBlockEntity(String email, Long userId) {
        return blockRepository.findByBlockedByEmailAndBlockedUserId(email, userId)
                .orElseThrow(() -> new NotFoundException("block", "not found"));
    }

    @Cacheable(value = "blocks", key = "'userId:' + #userId + ':email:' + #email")
    public BlockDto getUserBlock(String email, Long userId) {
        return blockMapper.toDto(getUserBlockEntity(email, userId));
    }

    @Cacheable(value = "isBlocked", key = "'userId:' + #userId + ':email:' + #email")
    public boolean isBlocked(String email, Long userId) {
        return blockRepository.existsByBlockedByEmailAndBlockedUserId(email, userId) ||
                blockRepository.existsByBlockedByIdAndBlockedUserEmail(userId, email);
    }

    private boolean existsByBlockedByEmailAndBlockedUserId(String email, Long userId) {
        return blockRepository.existsByBlockedByEmailAndBlockedUserId(email, userId);
    }

    public List<Block> getUserBlockEntities(String email) {
        return blockRepository.findAllByBlockedByEmail(email);
    }

    @Cacheable(value = "blocks", key = "'email:' + #email")
    public List<BlockDto> getUserBlocks(String email) {
        return blockMapper.toDtoList(getUserBlockEntities(email));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "blocks", key = "'email:' + #email"),
            @CacheEvict(value = "blocks", key = "'userId:' + #blockedUserId + ':email:' + #email"),
            @CacheEvict(value = "isBlocked", key = "'userId:' + #blockedUserId + ':email:' + #email")
    })
    public Block createBlock(String email, Long blockedUserId) {
        if (existsByBlockedByEmailAndBlockedUserId(email, blockedUserId)) {
            throw new BadRequestException("message", "User already blocked");
        }
        User blockedBy = userService.getUserEntityByEmail(email);
        User blockedUser = userService.getUserEntity(blockedUserId);
        if (blockedBy.getId().equals(blockedUser.getId())) {
            throw new BadRequestException("message", "User cannot block themselves");
        }
        Block block = Block.builder()
                .blockedBy(blockedBy)
                .blockedUser(blockedUser)
                .build();
        return blockRepository.save(block);
    }

    @Transactional
    public void deleteBlock(String email, Long blockId) {
        Block block = getBlockEntity(email, blockId);
        evictBlockCaches(email, blockId, block);
        blockRepository.delete(block);
    }

    private void evictBlockCaches(String email, Long blockId, Block block) {
        Cache blocksCache = cacheManager.getCache("blocks");
        if (blocksCache != null) {
            blocksCache.evict("id:" + blockId + ":email:" + email);
            blocksCache.evict("email:" + email);
            blocksCache.evict("userId:" + block.getBlockedUser().getId() + ":email:" + email);
        }
        Cache isBlockedCache = cacheManager.getCache("isBlocked");
        if (isBlockedCache != null) {
            isBlockedCache.evict("userId:" + block.getBlockedUser().getId() + ":email:" + email);
        }
    }
}