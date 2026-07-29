package com.bypass.bypasstransers.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Define rate limits per endpoint
    private static final int LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 15;

    private static final int PASSWORD_RESET_ATTEMPTS = 3;
    private static final int PASSWORD_RESET_WINDOW_HOURS = 1;

    private static final int API_REQUESTS = 100;
    private static final int API_WINDOW_MINUTES = 1;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        String key = clientIp;

        // Apply different rate limits based on endpoint
        if (path.contains("/login") && request.getMethod().equals("POST")) {
            return checkRateLimit(response, key, LOGIN_ATTEMPTS, LOGIN_WINDOW_MINUTES, "Login");
        } else if (path.contains("/forgot-password") || path.contains("/reset")) {
            return checkRateLimit(response, key, PASSWORD_RESET_ATTEMPTS, PASSWORD_RESET_WINDOW_HOURS * 60, "Password Reset");
        } else if (path.contains("/api/")) {
            return checkRateLimit(response, key, API_REQUESTS, API_WINDOW_MINUTES, "API");
        }

        return true;
    }

    private boolean checkRateLimit(HttpServletResponse response, String key, int tokens, int windowMinutes, String endpoint) {
        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket(tokens, windowMinutes));

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            logger.warn("Rate limit exceeded for - endpoint: {}, ip: {}", endpoint, key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            } catch (Exception e) {
                logger.error("Failed to write rate limit response", e);
            }
            return false;
        }
    }

    private Bucket createNewBucket(int tokens, int windowMinutes) {
        Bandwidth limit = Bandwidth.classic(tokens, Refill.intervally(tokens, Duration.ofMinutes(windowMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}