package com.chatter.chatter.integration;

import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.request.OtpSendRequest;
import com.chatter.chatter.request.OtpVerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OtpIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User user;

    @BeforeEach
    public void setup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                        .email("test@example.com")
                        .password("testPassword")
                        .username("testUser")
                        .build());
    }

    @Test
    void verifyOtp_ShouldReturnBadRequest_WhenInvalidToken() {
        String token = UUID.randomUUID().toString();
        OtpVerificationRequest request = new OtpVerificationRequest(token, user.getEmail());
        webClient
                .post()
                .uri("/api/otp/verification")
                .body(Mono.just(request),  OtpVerificationRequest.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void verifyOtp_ShouldSucceed_WhenValidToken() {
        String otp = "123456";
        redisTemplate.opsForValue().set("otp:email:" + user.getEmail(), otp);

        OtpVerificationRequest request = new OtpVerificationRequest(otp, user.getEmail());

        webClient.post()
                .uri("/api/otp/verification")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").exists()
                .jsonPath("$.token").value(token -> {
                    String tokenStr = (String) token;
                    Object verifiedEmail = redisTemplate.opsForValue().get("otp:verified:" + tokenStr);
                    assertEquals(user.getEmail(), verifiedEmail);
                    assertNull(redisTemplate.opsForValue().get("otp:email:" + user.getEmail()));
                });
    }

    @Test
    void verifyOtp_ShouldReturnTooManyRequests_WhenRateLimitExceeded() {
        OtpVerificationRequest request = new OtpVerificationRequest(UUID.randomUUID().toString(), user.getEmail());

        for (int i = 0; i < 3; i++) {
            webClient.post()
                    .uri("/api/otp/verification")
                    .body(Mono.just(request),  OtpVerificationRequest.class)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        webClient.post()
                .uri("/api/otp/verification")
                .body(Mono.just(request),  OtpVerificationRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void sendOtp_ShouldReturnBadRequest_WhenValidRequest() {
        OtpSendRequest request = new OtpSendRequest(user.getEmail());
        webClient
                .post()
                .uri("/api/otp/send-otp")
                .body(Mono.just(request),  OtpSendRequest.class)
                .exchange()
                .expectStatus().isOk();

        String otpKey = "otp:email:" + user.getEmail();
        Object storedOtp = redisTemplate.opsForValue().get(otpKey);
        assertNotNull(storedOtp);
    }

    @Test
    void sendOtp_ShouldReturnTooManyRequests_WhenRateLimitExceeded() {
        OtpSendRequest request = new OtpSendRequest(user.getEmail());

        webClient.post()
                .uri("/api/otp/send-otp")
                .body(Mono.just(request),  OtpSendRequest.class)
                .exchange()
                .expectStatus().isOk();

        webClient.post()
                .uri("/api/otp/send-otp")
                .body(Mono.just(request),  OtpSendRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }




}
