package com.chatter.chatter.service;

import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.NotFoundException;
import com.chatter.chatter.model.RefreshToken;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.RefreshTokenRepository;
import com.chatter.chatter.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${security.jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public TokenDto generateToken(String username) {
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("isRefresh", false);
        String accessToken = buildToken(username, accessClaims, accessTokenExpiration);

        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("isRefresh", true);
        String refreshToken = buildToken(username, refreshClaims, refreshTokenExpiration);

        User user = userRepository.findByEmail(username).orElseThrow(() -> new BadRequestException("message", "user not found"));
        RefreshToken newToken = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .build();
        refreshTokenRepository.save(newToken);
        return new TokenDto(accessToken, refreshToken);
    }

    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        Map<String, Object> accessClaims = new HashMap<>();
        Map<String, Object> refreshClaims = new HashMap<>();
        String username;
        try {
            username = extractUsername(refreshToken);
        }
        catch (MalformedJwtException e) {
            throw new BadRequestException("refreshToken", "Invalid token format");
        }
        catch (ExpiredJwtException e) {
            throw new BadRequestException("refreshToken", "Token has expired");
        }

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new BadRequestException("message", "Invalid refresh token"));
        refreshTokenRepository.delete(token);
        refreshTokenRepository.flush();

        User user = userRepository.findByEmail(username).orElseThrow(() -> new NotFoundException("message", "User not found"));

        accessClaims.put("isRefresh", false);
        String accessToken = buildToken(username, accessClaims, accessTokenExpiration);

        refreshClaims.put("isRefresh", true);
        String newRefreshToken = buildToken(username, refreshClaims, refreshTokenExpiration);

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshToken)
                .build();
        refreshTokenRepository.save(newToken);
        return new TokenDto(accessToken, newRefreshToken);
    }

    private String buildToken(String username, Map<String, Object> claims, Long expiration) {
        return Jwts.builder()
            .claims()
            .add(claims)
            .id(UUID.randomUUID().toString())
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
        try {
            String username = extractUsername(token);
            return userDetails.getUsername().equals(username) && !isTokenExpired(token) && !extractIsRefresh(token);
        }
        catch (MalformedJwtException | ExpiredJwtException e) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return userDetails.getUsername().equals(username) && !isTokenExpired(token) && extractIsRefresh(token);
        }
        catch (MalformedJwtException |  ExpiredJwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
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
