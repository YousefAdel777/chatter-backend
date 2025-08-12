package com.chatter.chatter.service;

import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.RefreshTokenRepository;
import com.chatter.chatter.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public TokenDto generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("isRefresh", false);
        String accessToken = buildToken(username, claims, 60 * 60);
        claims.put("isRefresh", true);
        String refreshToken = buildToken(username, claims, 60 * 60 * 24 * 30);
        User user = userRepository.findByEmail(username).orElseThrow();
        RefreshToken newToken = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .build();
        refreshTokenRepository.save(newToken);
        return new TokenDto(accessToken, refreshToken);
    }

    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        Map<String, Object> claims = new HashMap<>();
        String username = extractUsername(refreshToken);

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new BadRequestException("message", "Invalid refresh token"));
        refreshTokenRepository.delete(token);
        refreshTokenRepository.flush();

        User user = userRepository.findByEmail(username).orElseThrow(() -> new NotFoundException("message", "User not found"));

        claims.put("isRefresh", false);
        String accessToken = buildToken(username, claims, 60 * 60);

        claims.put("isRefresh", true);
        String newRefreshToken = buildToken(username, claims, 60 * 60 * 24 * 30);

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshToken)
                .build();
        refreshTokenRepository.save(newToken);
        return new TokenDto(accessToken, newRefreshToken);
    }

    private String buildToken(String username, Map<String, Object> claims, long expiration) {
        return Jwts.builder()
            .claims()
            .add(claims)
            .subject(username)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + (expiration * 1000)))
            .and()
            .signWith(getKey())
            .compact();
    }

    private SecretKey getKey() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(bytes);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimResolver) {
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return userDetails.getUsername().equals(username) && !isTokenExpired(token) && !extractIsRefresh(token);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return userDetails.getUsername().equals(username) && !isTokenExpired(token) && extractIsRefresh(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public boolean extractIsRefresh(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("isRefresh", Boolean.class);
    }

    public String generateCode(TokenDto tokenDto) {
        String code = UUID.randomUUID().toString();
        String key = "token_code:" + code;
        redisTemplate.opsForValue().set(key, tokenDto, 15, TimeUnit.MINUTES);
        return code;
    }

    public TokenDto getTokensByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        String key = "token_code:" + code;
        return (TokenDto) redisTemplate.opsForValue().getAndDelete(key);
    }
}
