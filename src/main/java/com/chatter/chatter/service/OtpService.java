package com.chatter.chatter.service;

import com.chatter.chatter.config.RabbitMQConfig;
import com.chatter.chatter.event.OtpEmailEvent;
import com.chatter.chatter.exception.BadRequestException;
import com.chatter.chatter.exception.TooManyRequestsException;
import com.chatter.chatter.model.User;
import com.chatter.chatter.repository.UserRepository;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;

    private final Supplier<BucketConfiguration> bucketSupplier;
    private final ProxyManager<String> proxyManager;

    @Autowired
    public OtpService(
            RedisTemplate<String, Object> redisTemplate,
            RabbitTemplate rabbitTemplate,
            UserRepository userRepository,
            @Qualifier("otpBucketConfiguration") Supplier<BucketConfiguration> bucketSupplier,
            ProxyManager<String> proxyManager
    ) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.userRepository = userRepository;
        this.bucketSupplier = bucketSupplier;
        this.proxyManager = proxyManager;
    }

    private static final SecureRandom random = new SecureRandom();
    private static final String OTP_KEY = "otp:email:";
    private static final String VERIFIED_KEY = "otp:verified:";
    private static final Long otpExpiry = 10 * 60L;
    private static final String OTP_RATE_LIMIT_PREFIX = "otp:rate:";

    private void validateRateLimit(String email) {
        Bucket bucket = proxyManager.builder().build(OTP_RATE_LIMIT_PREFIX + email, bucketSupplier);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long timeForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            throw new TooManyRequestsException("Too many requests", timeForRefill);
        }
    }

    private String generateOtp(String email) {
        validateRateLimit(email);
        String otp = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(OTP_KEY + email, otp, Duration.ofSeconds(otpExpiry));
        return otp;
    }

    public void generateOtpAndSend(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;
        generateOtpAndSend(user.getEmail(), user.getUsername());
    }

    public void generateOtpAndSend(String email, String username) {
        String otp = generateOtp(email);
        rabbitTemplate.convertAndSend(RabbitMQConfig.OTP_EMAILS, new OtpEmailEvent(email, otp, username));
    }

    public String verifyOtp(String email, String otp) {
        validateRateLimit(email);
        String storedOtp = (String) redisTemplate.opsForValue().get(OTP_KEY + email);
        if (storedOtp == null || !MessageDigest.isEqual(storedOtp.getBytes(), otp.getBytes())) {
            throw new BadRequestException("otp", "invalid otp");
        }
        String token = UUID.randomUUID().toString();
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                ops.multi();
                ops.delete(OTP_KEY + email);
                ops.opsForValue().set(VERIFIED_KEY + token, email, Duration.ofSeconds(otpExpiry));
                return ops.exec();
            }
        });
        return token;
    }

    public String getVerifiedEmailByToken(String token) {
        String stored = (String) redisTemplate.opsForValue().get(VERIFIED_KEY + token);
        if (stored == null) {
            throw new BadRequestException("token", "invalid token");
        }
        return stored;
    }

    public void deleteToken(String token) {
        redisTemplate.delete(VERIFIED_KEY + token);
    }

}
