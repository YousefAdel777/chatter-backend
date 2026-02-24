package com.chatter.chatter.integration.repository;

import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.RefreshTokenRepository;
import com.chatter.chatter.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class RefreshTokenRepositoryTests {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    public void setup() {
        user = userRepository.save(User.builder()
                .email("testEmail@example.com")
                .username("testUsername")
                .password("testPassword")
                .build());
    }

    @Test
    void shouldSaveRefreshToken_WhenValid() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();
        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        assertNotNull(savedRefreshToken.getId());
        assertEquals(refreshToken.getToken(), savedRefreshToken.getToken());
        assertEquals(user.getId(), savedRefreshToken.getUser().getId());
    }

    @Test
    void shouldThrow_WhenSavingDuplicateTokens() {
        RefreshToken refreshToken1 = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();

        RefreshToken refreshToken2 = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken1);
        assertThrows(DataIntegrityViolationException.class, () -> refreshTokenRepository.save(refreshToken2));
    }

    @Test
    void shouldFindRefreshToken_WhenTokenExists() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
        RefreshToken foundRefreshToken = refreshTokenRepository.findByToken(refreshToken.getToken()).orElse(null);
        assertNotNull(foundRefreshToken);
        assertEquals(refreshToken.getToken(), foundRefreshToken.getToken());
        assertEquals(user.getId(), foundRefreshToken.getUser().getId());
    }

    @Test
    void shouldReturnEmpty_WhenTokenNotExists() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
        RefreshToken foundRefreshToken = refreshTokenRepository.findByToken("invalidToken").orElse(null);
        assertNull(foundRefreshToken);
    }

    @Test
    void shouldFindRefreshToken_WhenTokenAndUserEmailMatch() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
        RefreshToken foundRefreshToken = refreshTokenRepository.findByTokenAndUserEmail(refreshToken.getToken(), user.getEmail()).orElse(null);
        assertNotNull(foundRefreshToken);
        assertEquals(refreshToken.getToken(), foundRefreshToken.getToken());
        assertEquals(user.getId(), foundRefreshToken.getUser().getId());
    }

    @Test
    void shouldReturnEmpty_WhenTokenOrUserEmailDoesNotMatch() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("token")
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
        RefreshToken foundRefreshToken = refreshTokenRepository.findByTokenAndUserEmail("invalidToken", user.getEmail()).orElse(null);
        assertNull(foundRefreshToken);
    }

}
