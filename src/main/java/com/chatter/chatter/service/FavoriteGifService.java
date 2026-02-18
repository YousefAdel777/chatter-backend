package com.chatter.chatter.service;

import com.chatter.chatter.dto.FavoriteGifDto;
import com.chatter.chatter.dto.PageDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.FavoriteGifMapper;
import com.chatter.chatter.model.FavoriteGif;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.FavoriteGifRepository;
import com.chatter.chatter.request.FavoriteGifRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteGifService {

    private final FavoriteGifRepository favoriteGifRepository;
    private final FavoriteGifMapper favoriteGifMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;
    private final UserService userService;

    @Value("${spring.cache.redis.time-to-live}")
    private Long timeToLive;

    @Cacheable(value = "favorite-gifs-page", key = "'userId:' + #userId + ':pageNumber:' + #pageable.pageNumber + ':pageSize:' + #pageable.pageSize + ':sort:' + (#pageable.sort != null ? #pageable.sort : 'unsorted')")
    public PageDto<FavoriteGifDto> getFavoriteGifs(Long userId, Pageable pageable) {
        Page<FavoriteGifDto> favoriteGifsPage = favoriteGifRepository.findAllByUserId(userId, pageable).map(favoriteGifMapper::toDto);
        return new PageDto<>(favoriteGifsPage.getContent(), favoriteGifsPage.getTotalElements());
    }

    public List<FavoriteGifDto> getFavoriteGifs(Long userId, List<String> gifIds) {
        Map<String, FavoriteGifDto> results = new LinkedHashMap<>();
        List<String> keys = gifIds.stream().map(id -> {
            results.put(id, null);
            return "favorite-gifs::userId:" + userId + ":gifId:" + id;
        }).collect(Collectors.toList());
        List<Object> cacheResults = redisTemplate.opsForValue().multiGet(keys);
        if (cacheResults != null) {
            for (Object object : cacheResults) {
                if (object !=  null) {
                    FavoriteGifDto favoriteGifDto = (FavoriteGifDto) object;
                    results.put(favoriteGifDto.getGifId(), favoriteGifDto);
                }
            }
        }
        List<String> idsNotFound = results.keySet().stream().filter(id -> results.get(id) == null).collect(Collectors.toList());
        if (!idsNotFound.isEmpty()) {
            List<FavoriteGif> foundGifs = favoriteGifRepository.findAllByUserIdAndGifIdIn(userId, idsNotFound);
            Map<String, FavoriteGifDto> updatedCache = new HashMap<>();
            for (FavoriteGif favoriteGif : foundGifs) {
                FavoriteGifDto favoriteGifDto = favoriteGifMapper.toDto(favoriteGif);
                results.put(favoriteGifDto.getGifId(), favoriteGifDto);
                updatedCache.put("favorite-gifs::userId:" + userId + ":gifId:" + favoriteGif.getGifId(), favoriteGifDto);
            }
            if (!updatedCache.isEmpty()) {
                redisTemplate.executePipelined(new SessionCallback<>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                        updatedCache.forEach((key, value) -> ops.opsForValue().set(key, value, timeToLive, TimeUnit.MILLISECONDS));
                        return null;
                    }
                });
            }
        }
        return results.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    @Cacheable(value = "favorite-gif-status", key = "'userId:' + #userId + ':gifId:' + #gifId")
    public boolean getGifStatus(Long userId, String gifId) {
        return favoriteGifRepository.existsByUserIdAndGifId(userId, gifId);
    }

    @Transactional
    @CacheEvict(value = "favorite-gif-status", key = "'userId:' + #userId + ':gifId:' + #request.getGifId()")
    public FavoriteGif createFavoriteGif(Long userId, FavoriteGifRequest request) {
        if (favoriteGifRepository.existsByUserIdAndGifId(userId, request.getGifId())) {
            throw new BadRequestException("gifId", "Gif already in favorites");
        }
        User user = userService.getReference(userId);
        FavoriteGif favoriteGif = FavoriteGif.builder()
                .user(user)
                .gifId(request.getGifId())
                .build();
        FavoriteGif created = favoriteGifRepository.save(favoriteGif);
        cacheService.evictCacheSynchronized("favorite-gifs-page::userId:" + userId + "*");
        return created;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "favorite-gifs", key = "'userId:' + #userId + ':gifId:' + #gifId"),
        @CacheEvict(value = "favorite-gif-status", key = "'userId:' + #userId + ':gifId:' + #gifId")
    })
    public void deleteFavoriteGif(Long userId, String gifId) {
        int count = favoriteGifRepository.deleteByGifIdAndUserId(gifId, userId);
        if (count == 0) {
            throw new NotFoundException("favoriteGif", "not found");
        }
        cacheService.evictCacheSynchronized("favorite-gifs-page::userId:" + userId + "*");
    }

}
