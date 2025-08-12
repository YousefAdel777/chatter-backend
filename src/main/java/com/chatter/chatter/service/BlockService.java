package com.chatter.chatter.service;

import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
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

    @Cacheable(value = "blocks", key = "'block:id:' + #blockId + ':email:' + #email")
    public Block getBlock(String email, Long blockId) {
        return blockRepository.findByIdAndBlockedByEmail(blockId, email)
                .orElseThrow(() -> new NotFoundException("block", "not found"));
    }

    @Cacheable(value = "blocks", key = "'block:user:' + #userId + ':email:' + #email")
    public Block getUserBlock(String email, Long userId) {
        return blockRepository.findByBlockedByEmailAndBlockedUserId(email, userId)
                .orElseThrow(() -> new NotFoundException("block", "not found"));
    }

    @Cacheable(value = "blocks", key = "'isBlocked:user:' + #userId + ':email:' + #email")
    public boolean isBlocked(String email, Long userId) {
        return blockRepository.existsByBlockedByEmailAndBlockedUserId(email, userId) ||
                blockRepository.existsByBlockedByIdAndBlockedUserEmail(userId, email);
    }

    @Cacheable(value = "blocks", key = "'exists:user:' + #userId + ':email:' + #email")
    private boolean existsByBlockedByEmailAndBlockedUserId(String email, Long userId) {
        return blockRepository.existsByBlockedByEmailAndBlockedUserId(email, userId);
    }

    @Cacheable(value = "blocks", key = "'blocks:email:' + #email")
    public List<Block> getUserBlocks(String email) {
        return blockRepository.findAllByBlockedByEmail(email);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "blocks", key = "'blocks:email:' + #email"),
            @CacheEvict(value = "blocks", key = "'block:user:' + #blockedUserId + ':email:' + #email"),
            @CacheEvict(value = "blocks", key = "'exists:user:' + #blockedUserId + ':email:' + #email"),
            @CacheEvict(value = "blocks", key = "'isBlocked:user:' + #blockedUserId + ':email:' + #email")
    })
    public Block createBlock(String email, Long blockedUserId) {
        if (existsByBlockedByEmailAndBlockedUserId(email, blockedUserId)) {
            throw new BadRequestException("message", "User already blocked");
        }
        User blockedBy = userService.getUserEntityByEmail(email);
        User blockedUser = userService.getUserEntity(blockedUserId);
        Block block = Block.builder()
                .blockedBy(blockedBy)
                .blockedUser(blockedUser)
                .build();
        return blockRepository.save(block);
    }

    @Transactional
    public void deleteBlock(String email, Long blockId) {
        Block block = getBlock(email, blockId);
        evictBlockCaches(email, blockId, block);
        blockRepository.delete(block);
    }

    private void evictBlockCaches(String email, Long blockId, Block block) {
        Cache cache = cacheManager.getCache("blocks");
        if (cache != null) {
            cache.evict("block:id:" + blockId + ":email:" + email);
            cache.evict("blocks:email:" + email);
            cache.evict("block:user:" + block.getBlockedUser().getId() + ":email:" + email);
            cache.evict("isBlocked:user:" + block.getBlockedUser().getId() + ":email:" + email);
            cache.evict("exists:user:" + block.getBlockedUser().getId() + ":email:" + email);
        }
    }
}