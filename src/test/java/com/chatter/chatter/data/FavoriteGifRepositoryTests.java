package com.chatter.chatter.data;

import com.chatter.chatter.config.JpaAuditingConfig;
import com.chatter.chatter.model.FavoriteGif;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.FavoriteGifRepository;
import com.chatter.chatter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
public class FavoriteGifRepositoryTests {

    @Autowired
    private FavoriteGifRepository favoriteGifRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private String gifId1;
    private String gifId2;

    @BeforeEach
    public void setup() {
        favoriteGifRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .password("testPassword")
                .username("testUser")
                .email("test@example.com")
                .build());
        gifId1 = UUID.randomUUID().toString();
        gifId2 = UUID.randomUUID().toString();
    }

    @Test
    public void shouldCreateFavoriteGif_WhenValid() {
        FavoriteGif favoriteGif = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();

        FavoriteGif created = favoriteGifRepository.save(favoriteGif);
        List<FavoriteGif> favoriteGifs = favoriteGifRepository.findAll();

        assertNotNull(created);
        assertEquals(created.getGifId(), gifId1);
        assertEquals(created.getUser(), user);
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getId());
        assertEquals(1, favoriteGifs.size());
    }

    @Test
    public void shouldNotSave_WhenDuplicateGifIds() {
        FavoriteGif favoriteGif = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();

        FavoriteGif duplicate = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();

        favoriteGifRepository.saveAndFlush(favoriteGif);
        assertThrows(DataIntegrityViolationException.class, () -> {
            favoriteGifRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    public void shouldDeleteFavoriteGif_WhenFound() {
        FavoriteGif favoriteGif = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();

        favoriteGifRepository.save(favoriteGif);
        int res = favoriteGifRepository.deleteByGifIdAndUserId(gifId1, user.getId());

        assertEquals(1, res);
        assertEquals(0, favoriteGifRepository.findAll().size());
    }

    @Test
    public void shouldNotDeleteFavoriteGif_WhenNotFound() {
        FavoriteGif favoriteGif = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();

        favoriteGifRepository.save(favoriteGif);
        int res = favoriteGifRepository.deleteByGifIdAndUserId(gifId2, user.getId());

        assertEquals(0, res);
        assertEquals(1, favoriteGifRepository.findAll().size());
    }

    @Test
    public void findAllByUserIdAndGifIdIn_ShouldReturnFavoriteGifsByGifIds() {
        FavoriteGif favoriteGif1 = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();

        FavoriteGif favoriteGif2 = FavoriteGif.builder()
                .gifId(gifId2)
                .user(user)
                .build();
        favoriteGifRepository.saveAll(List.of(favoriteGif1, favoriteGif2));

        List<FavoriteGif> results = favoriteGifRepository.findAllByUserIdAndGifIdIn(user.getId(), List.of(gifId1));
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(favoriteGif1.getGifId(), results.getFirst().getGifId());
    }

    @Test
    public void findAllByUserId_ShouldReturnPagedResults_WhenUserHasMultipleFavorites() {
        FavoriteGif favoriteGif1 = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();
        FavoriteGif favoriteGif2 = FavoriteGif.builder()
                .gifId(gifId2)
                .user(user)
                .build();
        favoriteGifRepository.saveAll(List.of(favoriteGif1, favoriteGif2));

        Pageable pageable = PageRequest.of(0, 1);
        Page<FavoriteGif> result = favoriteGifRepository.findAllByUserId(user.getId(), pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getTotalPages());
        assertTrue(result.hasNext());
    }

    @Test
    public void findAllByUserId_ShouldReturnEmptyPage_WhenUserHasNoFavorites() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FavoriteGif> result = favoriteGifRepository.findAllByUserId(user.getId(), pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void existsByUserIdAndGifId_ShouldReturnTrue_WhenExists() {
        FavoriteGif favoriteGif1 = FavoriteGif.builder()
                .gifId(gifId1)
                .user(user)
                .build();
        favoriteGifRepository.save(favoriteGif1);

        boolean result = favoriteGifRepository.existsByUserIdAndGifId(user.getId(), gifId1);
        assertTrue(result);
    }

    @Test
    public void existsByUserIdAndGifId_ShouldReturnFalse_WhenNoMatchExists() {
        FavoriteGif favoriteGif2 = FavoriteGif.builder()
                .gifId(gifId2)
                .user(user)
                .build();
        favoriteGifRepository.save(favoriteGif2);

        boolean result = favoriteGifRepository.existsByUserIdAndGifId(user.getId(), gifId1);
        assertFalse(result);
    }
}
