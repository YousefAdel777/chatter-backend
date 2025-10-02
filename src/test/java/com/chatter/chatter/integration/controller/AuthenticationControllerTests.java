package com.chatter.chatter.integration.controller;

import com.chatter.chatter.dto.TokenDto;
import com.chatter.chatter.integration.BaseIntegrationTest;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.RefreshTokenRepository;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.RefreshTokenRequest;
import com.chatter.chatter.request.UserLoginRequest;
import com.chatter.chatter.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationControllerTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private User user;

    private String accessToken;

    private String refreshToken;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        refreshTokenRepository.deleteAll();

        user = createUser("test@example.com");
        TokenDto tokenDto = jwtService.generateToken(user.getEmail());
        accessToken = tokenDto.getAccessToken();
        refreshToken = tokenDto.getRefreshToken();
    }

    @Test
    void login_ShouldLoginUserAndReturnTokenDto_WhenValidEmailAndPassword() throws Exception {
        refreshTokenRepository.deleteAll();
        UserLoginRequest request = new UserLoginRequest(user.getEmail(), user.getPassword());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").exists())
                        .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void login_ShouldThrow_WhenWrongEmail() throws Exception {
        UserLoginRequest request = new UserLoginRequest("wrongEmail@example.com", user.getPassword());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldThrow_WhenWrongPassword() throws Exception {
        UserLoginRequest request = new UserLoginRequest(user.getEmail(), "wrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldThrowBadRequest_WhenInvalidEmail() throws Exception {
        UserLoginRequest request = new UserLoginRequest("invalidEmail", user.getPassword());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldThrowBadRequest_WhenEmptyPassword() throws Exception {
        UserLoginRequest request = new UserLoginRequest(user.getEmail(), "");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldThrowBadRequest_WhenEmptyEmail() throws Exception {
        UserLoginRequest request = new UserLoginRequest("", user.getPassword());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_shouldReturnNewAccessTokenAndRefreshToken_WhenValidRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.refreshToken").value(not(refreshToken)))
                        .andExpect(jsonPath("$.accessToken").value(not(accessToken)));
    }

    @Test
    void refresh_shouldThrowBadRequest_WhenInvalidRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalidRefreshToken");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_shouldThrowBadRequest_WhenExpiredRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(generateExpiredToken(user.getEmail(), true));
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_shouldThrowBadRequest_WhenUserNotInDatabase() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        userRepository.delete(user);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldThrowBadRequest_WhenProvidedTokenIsNotRefresh() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(accessToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void logout_ShouldLogoutUser_WhenValidRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
    }

    @Test
    void logout_shouldThrowBadRequest_WhenNotSameUserIsAuthenticated() throws Exception {
        User user1 = createUser("test1@example.com");
        User user2 = createUser("test2@example.com");
        TokenDto tokenDto1 = jwtService.generateToken(user1.getEmail());
        TokenDto tokenDto2 = jwtService.generateToken(user2.getEmail());
        RefreshTokenRequest request = new RefreshTokenRequest(tokenDto1.getRefreshToken());
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenDto2.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void logout_shouldThrowBadRequest_WhenInvalidRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalidRefreshToken");
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void logout_shouldThrowBadRequest_WhenExpiredRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(generateExpiredToken(user.getEmail(), true));
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void logout_ShouldThrowBadRequest_WhenProvidedTokenIsNotRefresh() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(accessToken);
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    private User createUser(String email) {
        User createdUser = userRepository.save(User.builder()
                .username("testUsername")
                .email(email)
                .password(passwordEncoder.encode("testPassword"))
                .build());
        createdUser.setPassword("testPassword");
        return createdUser;
    }

    private String generateExpiredToken(String email, boolean isRefresh) {
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(bytes);
        return Jwts.builder()
                .claim("isRefresh", isRefresh)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();
    }

}
