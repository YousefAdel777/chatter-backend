package com.chatter.chatter.unit.service;

import com.chatter.chatter.event.OtpEmailEvent;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.TooManyRequestsException;
import com.chatter.chatter.repository.UserRepository;
import com.chatter.chatter.service.OtpService;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTests {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProxyManager<String> proxyManager;
    @Mock
    private Supplier<BucketConfiguration> bucketSupplier;

    @Mock
    private RemoteBucketBuilder<String> bucketBuilder;
    @Mock
    private BucketProxy bucket;
    @Mock
    private ConsumptionProbe probe;

    @InjectMocks
    private OtpService otpService;

    private final String email = "test@example.com";
    private final String username = "testUsername";

    @Test
    void generateOtpAndSend_ShouldSucceed_WhenLimitNotExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        setupRateLimit(true);
        otpService.generateOtpAndSend(email, username);

        verify(valueOperations).set(eq("otp:email:" + email), anyString(), any(Duration.class));
        verify(rabbitTemplate).convertAndSend(anyString(), any(OtpEmailEvent.class));
    }

    @Test
    void generateOtpAndSend_ShouldThrowException_WhenLimitExceeded() {
        setupRateLimit(false);
        assertThrows(TooManyRequestsException.class, () ->
                otpService.generateOtpAndSend(email, username)
        );
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void verifyOtp_ShouldReturnToken_WhenCorrectOtp() {
        String otp = "123456";
        setupRateLimit(true);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:email:" + email)).thenReturn(otp);

        String token = otpService.verifyOtp(email, otp);
        assertNotNull(token);
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void verifyOtp_ShouldThrowsException_WhenWrongOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        setupRateLimit(true);
        when(valueOperations.get("otp:email:" + email)).thenReturn("123456");

        assertThrows(BadRequestException.class, () ->
                otpService.verifyOtp(email, "wrong-otp")
        );
    }

    @Test
    void deleteToken_ShouldDeleteToken() {
        String token = UUID.randomUUID().toString();
        otpService.deleteToken(token);
        verify(redisTemplate).delete("otp:verified:" + token);
    }

    @Test
    void getVerifiedEmailByToken_ShouldReturnEmail_WhenValidToken() {
        String token = UUID.randomUUID().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:verified:" + token)).thenReturn(email);

        String verifiedEmail = otpService.getVerifiedEmailByToken(token);
        assertEquals(email, verifiedEmail);
    }

    @Test
    void getVerifiedEmailByToken_ShouldThrowException_WhenInvalidToken() {
        String token = UUID.randomUUID().toString();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:verified:" + token)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> otpService.getVerifiedEmailByToken(token));
    }

    private void setupRateLimit(boolean allowed) {
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(allowed);
        if (!allowed) {
            when(probe.getNanosToWaitForRefill()).thenReturn(5000000000L); // 5 seconds
        }
    }

}
