package com.chatter.chatter.interceptor;

import com.chatter.chatter.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Principal principal = request.getUserPrincipal();
        String email = principal == null ? null : principal.getName();
        String ip = request.getRemoteAddr();
        Bucket bucket = rateLimitingService.resolveBucket(email, ip);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return true;
        }
        long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        response.setContentType("application/json");
        response.setStatus(429);
        response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
        response.getWriter().write("{\"error\": \"Too Many Requests\"}");
        response.getWriter().flush();
        return false;
    }
}
