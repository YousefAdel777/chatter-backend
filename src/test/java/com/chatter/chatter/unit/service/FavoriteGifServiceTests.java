package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.FavoriteGifDto;
import com.chatter.chatter.dto.PageDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.mapper.FavoriteGifMapper;
import com.chatter.chatter.model.FavoriteGif;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.FavoriteGifRepository;
import com.chatter.chatter.request.FavoriteGifRequest;
import com.chatter.chatter.service.CacheService;
import com.chatter.chatter.service.FavoriteGifService;
import com.chatter.chatter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteGifServiceTests {

    @Mock
    private FavoriteGifRepository favoriteGifRepository;

    @Mock
    private UserService userService;

    @Mock
    private FavoriteGifMapper favoriteGifMapper;

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisTemplate<String, FavoriteGifDto> redisTemplate;

    @Mock
    private ValueOperations<String, FavoriteGifDto> valueOperations;

    @InjectMocks
    private FavoriteGifService favoriteGifService;

    private User user;
    private String gifId1;
    private String gifId2;
    private FavoriteGif favoriteGif1;
    private FavoriteGif favoriteGif2;
    private FavoriteGifDto favoriteGifDto1;
    private FavoriteGifDto favoriteGifDto2;

    @BeforeEach
    public void setUp() {
        gifId1 = "gif_id1";
        gifId2 = "gif_id2";
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        favoriteGif1 = FavoriteGif.builder()
                .id(1L)
                .gifId(gifId1)
                .user(user)
                .createdAt(Instant.now())
                .build();

        favoriteGif2 = FavoriteGif.builder()
                .id(2L)
                .gifId(gifId2)
                .user(user)
                .createdAt(Instant.now())
                .build();

        favoriteGifDto1 = FavoriteGifDto.builder()
                .id(1L)
                .gifId(gifId1)
                .userId(user.getId())
                .createdAt(Instant.now())
                .build();

        favoriteGifDto2 = FavoriteGifDto.builder()
                .id(2L)
                .gifId(gifId2)
                .userId(user.getId())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    public void getFavoriteGifs_ShouldReturnFavoriteGifsPageDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FavoriteGif> page = new PageImpl<>(List.of(favoriteGif1));
        when(favoriteGifRepository.findAllByUserId(user.getId(), pageable)).thenReturn(page);
        when(favoriteGifMapper.toDto(favoriteGif1)).thenReturn(favoriteGifDto1);

        PageDto<FavoriteGifDto> result = favoriteGifService.getFavoriteGifs(user.getId(), pageable);

        verify(favoriteGifRepository, times(1)).findAllByUserId(user.getId(), pageable);
        verify(favoriteGifMapper, times(1)).toDto(favoriteGif1);

        assertEquals(result.getTotalElements(), page.getTotalElements());
        assertEquals(result.getContent().size(), page.getContent().size());
        assertEquals(result.getContent().getFirst().getGifId(), favoriteGifDto1.getGifId());
        assertEquals(result.getContent().getFirst().getUserId(), favoriteGifDto1.getUserId());
    }

    @Test
    public void deleteFavoriteGif_ShouldDeleteFavoriteGifAndEvictCache_WhenFound()  {
        when(favoriteGifRepository.deleteByGifIdAndUserId(favoriteGif1.getGifId(), user.getId())).thenReturn(1);

        favoriteGifService.deleteFavoriteGif(user.getId(), favoriteGif1.getGifId());

        verify(cacheService, times(1)).evictCacheSynchronized("favorite-gifs-page::userId:" + user.getId() + "*");
    }

    @Test
    public void deleteFavoriteGif_ShouldThrow_WhenNotFound()  {
        when(favoriteGifRepository.deleteByGifIdAndUserId(favoriteGif1.getGifId(), user.getId())).thenReturn(0);

        assertThrows(NotFoundException.class, () -> favoriteGifService.deleteFavoriteGif(user.getId(), favoriteGif1.getGifId()));
        verify(cacheService, never()).evictCacheSynchronized(any());
    }

    @Test
    public void createFavoriteGif_ShouldCreateFavoriteGifAndEvictCache_WhenValid()  {
        when(favoriteGifRepository.existsByUserIdAndGifId(user.getId(), gifId1)).thenReturn(false);
        when(userService.getReference(user.getId())).thenReturn(user);
        when(favoriteGifRepository.save(any(FavoriteGif.class))).thenReturn(favoriteGif1);

        FavoriteGif result = favoriteGifService.createFavoriteGif(user.getId(), new FavoriteGifRequest(gifId1));

        assertEquals(result.getGifId(), favoriteGif1.getGifId());
        assertEquals(result.getUser(), user);
        assertEquals(result.getCreatedAt(), favoriteGif1.getCreatedAt());
        verify(cacheService, times(1)).evictCacheSynchronized("favorite-gifs-page::userId:" + user.getId() + "*");
    }

    @Test
    public void createFavoriteGif_ShouldThrow_WhenDuplicateGifId()  {
        when(favoriteGifRepository.existsByUserIdAndGifId(user.getId(), gifId1)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> favoriteGifService.createFavoriteGif(user.getId(), new FavoriteGifRequest(gifId1)));

        verify(cacheService, never()).evictCacheSynchronized(any());
        verify(favoriteGifRepository, never()).save(any(FavoriteGif.class));
    }

    @Test
    public void getFavoriteGifs_ShouldReturnAllFromCache_WhenAllKeysExist() {
        List<String> gifIds = Arrays.asList(gifId1, gifId2);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(anyList())).thenReturn(Arrays.asList(favoriteGifDto1, favoriteGifDto2));

        List<FavoriteGifDto> result = favoriteGifService.getFavoriteGifs(user.getId(), gifIds);

        assertEquals(2, result.size());
        verify(favoriteGifRepository, never()).findAllByUserIdAndGifIdIn(any(), anyList());
    }

    @Test
    public void getFavoriteGifs_ShouldFetchFromDbAndUpdateCache_WhenSomeKeysAreMissing() {
        List<String> gifIds = Arrays.asList(gifId1, gifId2);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(anyList())).thenReturn(Arrays.asList(favoriteGifDto1, null));
        when(favoriteGifRepository.findAllByUserIdAndGifIdIn(eq(user.getId()), anyList())).thenReturn(List.of(favoriteGif2));
        when(favoriteGifMapper.toDto(favoriteGif2)).thenReturn(favoriteGifDto2);

        List<FavoriteGifDto> result = favoriteGifService.getFavoriteGifs(user.getId(), gifIds);

        assertEquals(2, result.size());
        verify(redisTemplate).executePipelined(any(SessionCallback.class));
    }

    @Test
    public void getFavoriteGifs_ShouldReturnEmptyList_WhenIdsNotFoundInCacheOrDb() {
        List<String> gifIds = List.of("missing-id");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(anyList())).thenReturn(Collections.singletonList(null));
        when(favoriteGifRepository.findAllByUserIdAndGifIdIn(anyLong(), anyList())).thenReturn(Collections.emptyList());

        List<FavoriteGifDto> result = favoriteGifService.getFavoriteGifs(user.getId(), gifIds);

        assertEquals(0, result.size());
        verify(redisTemplate, never()).executePipelined(any(SessionCallback.class));
    }

    @Test
    public void getFavoriteGifs_ShouldHandleNullMultiGet_WhenRedisReturnsNull() {
        List<String> gifIds = List.of(gifId1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(anyList())).thenReturn(null);
        when(favoriteGifRepository.findAllByUserIdAndGifIdIn(eq(user.getId()), anyList())).thenReturn(List.of(favoriteGif1));
        when(favoriteGifMapper.toDto(favoriteGif1)).thenReturn(favoriteGifDto1);

        List<FavoriteGifDto> result = favoriteGifService.getFavoriteGifs(user.getId(), gifIds);

        assertEquals(1, result.size());
        verify(favoriteGifRepository).findAllByUserIdAndGifIdIn(eq(user.getId()), anyList());
    }

    @Test
    public void getGifStatus_ShouldReturnTrue_WhenExists() {
        when(favoriteGifRepository.existsByUserIdAndGifId(user.getId(), gifId1)).thenReturn(true);

        boolean result =  favoriteGifService.getGifStatus(user.getId(), gifId1);

        assertTrue(result);
    }

    @Test
    public void getGifStatus_ShouldReturnFalse_WhenNoMatchExists() {
        when(favoriteGifRepository.existsByUserIdAndGifId(user.getId(), gifId1)).thenReturn(false);

        boolean result =  favoriteGifService.getGifStatus(user.getId(), gifId1);

        assertFalse(result);
    }

}
