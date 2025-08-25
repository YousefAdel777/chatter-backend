package com.chatter.chatter.unit.service;

import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.RefreshTokenRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.service.JwtService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private JwtService jwtService;

    private User user;
    private SecretKey secretKey;
    private String base64SecretKey;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testUser")
                .password("password")
                .build();

        secretKey = Jwts.SIG.HS256.key().build();
        base64SecretKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        ReflectionTestUtils.setField(jwtService, "secretKey", base64SecretKey);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 86400L);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600L);
    }

    @Test
    void shouldGenerateToken() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TokenDto tokenDto = jwtService.generateToken(user.getEmail());

        assertNotNull(tokenDto.getAccessToken());
        assertNotNull(tokenDto.getRefreshToken());
        assertNotEquals(tokenDto.getAccessToken(), tokenDto.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void shouldGenerateToken_WithDifferentClaims() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));

        TokenDto tokenDto = jwtService.generateToken(user.getEmail());

        assertFalse(jwtService.extractIsRefresh(tokenDto.getAccessToken()));
        assertTrue(jwtService.extractIsRefresh(tokenDto.getRefreshToken()));
    }

    @Test
    void shouldRefreshToken() {
        String oldRefreshToken = generateTestToken(true);

        RefreshToken existingToken = RefreshToken.builder()
                .token(oldRefreshToken)
                .user(user)
                .build();

        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(Optional.of(existingToken));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TokenDto newToken = jwtService.refreshToken(oldRefreshToken);

        assertNotNull(newToken.getAccessToken());
        assertNotNull(newToken.getRefreshToken());
        assertNotEquals(oldRefreshToken, newToken.getRefreshToken());
        verify(refreshTokenRepository).delete(existingToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldThrowBadRequest_WhenInvalidRefreshToken() {
        assertThrows(BadRequestException.class, () -> jwtService.refreshToken("invalidToken"));
        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldThrowBadRequest_WhenTokenIsNotInDatabase() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> jwtService.refreshToken(generateTestToken(true)));
        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_WhenUserNotFoundDuringRefresh() {
        String refreshToken = generateTestToken(true);
        RefreshToken existingToken = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .build();

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(existingToken));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> jwtService.refreshToken(refreshToken));
        verify(refreshTokenRepository).delete(existingToken);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldValidateAccessToken() {
        TokenDto tokenDto = generateTestTokens();
        UserDetails userDetails = createUserDetails();

        assertTrue(jwtService.isAccessTokenValid(tokenDto.getAccessToken(), userDetails));
        assertFalse(jwtService.isAccessTokenValid(tokenDto.getRefreshToken(), userDetails));
    }

    @Test
    void shouldValidateRefreshToken() {
        TokenDto tokenDto = generateTestTokens();
        UserDetails userDetails = createUserDetails();

        assertTrue(jwtService.isRefreshTokenValid(tokenDto.getRefreshToken(), userDetails));
        assertFalse(jwtService.isRefreshTokenValid(tokenDto.getAccessToken(), userDetails));
    }

    @Test
    void shouldReturnFalse_WhenTokenExpired() {
        String expiredToken = generateExpiredToken();
        UserDetails userDetails = createUserDetails();

        assertFalse(jwtService.isAccessTokenValid(expiredToken, userDetails));
        assertFalse(jwtService.isRefreshTokenValid(expiredToken, userDetails));
    }

    @Test
    void shouldReturnFalse_WhenUsernameMismatch() {
        TokenDto tokenDto = generateTestTokens();
        UserDetails differentUser = org.springframework.security.core.userdetails.User
                .withUsername("different@example.com").password("pass").authorities("USER").build();

        assertFalse(jwtService.isAccessTokenValid(tokenDto.getAccessToken(), differentUser));
        assertFalse(jwtService.isRefreshTokenValid(tokenDto.getRefreshToken(), differentUser));
    }

    @Test
    void shouldExtractUsernameFromToken() {
        TokenDto tokenDto = generateTestTokens();

        assertEquals(user.getEmail(), jwtService.extractUsername(tokenDto.getAccessToken()));
        assertEquals(user.getEmail(), jwtService.extractUsername(tokenDto.getRefreshToken()));
    }

    @Test
    void shouldExtractIsRefreshFlag() {
        TokenDto tokenDto = generateTestTokens();

        assertFalse(jwtService.extractIsRefresh(tokenDto.getAccessToken()));
        assertTrue(jwtService.extractIsRefresh(tokenDto.getRefreshToken()));
    }

    @Test
    void shouldStoreAndRetrieveTokenCode() {
        TokenDto tokenDto = new TokenDto("access-test", "refresh-test");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(tokenDto);

        String generatedCode = jwtService.generateCode(tokenDto);
        assertNotNull(generatedCode);
        assertFalse(generatedCode.isEmpty());

        TokenDto retrieved = jwtService.getTokensByCode(generatedCode);
        assertEquals(tokenDto, retrieved);

        verify(valueOperations).set(
                eq("token_code:" + generatedCode),
                eq(tokenDto),
                eq(15L),
                eq(TimeUnit.MINUTES)
        );
        verify(valueOperations).getAndDelete("token_code:" + generatedCode);
    }

    @Test
    void shouldReturnNull_WhenCodeIsNullOrEmpty() {
        assertNull(jwtService.getTokensByCode(null));
        assertNull(jwtService.getTokensByCode(""));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void shouldReturnNull_WhenCodeNotFoundInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertNull(jwtService.getTokensByCode("nonexistent-code"));
    }

    @Test
    void shouldThrow_WhenTokenIsInvalid() {
        assertThrows(Exception.class, () -> jwtService.extractUsername("invalid.token.here"));
    }

    private TokenDto generateTestTokens() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return jwtService.generateToken(user.getEmail());
    }

    private String generateTestToken(boolean isRefresh) {
        return Jwts.builder()
                .claim("isRefresh", isRefresh)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();
    }

    private String generateExpiredToken() {
        return Jwts.builder()
                .claim("isRefresh", false)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(secretKey)
                .compact();
    }

    private UserDetails createUserDetails() {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("password")
                .authorities("USER")
                .build();
    }
}