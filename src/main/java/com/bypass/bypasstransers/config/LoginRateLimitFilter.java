package com.bypass.bypasstransers.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter that enforces per-IP rate limiting for login attempts.
 * Register this before UsernamePasswordAuthenticationFilter in the SecurityFilterChain.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final int tokens;
    private final Duration refillDuration;

    public LoginRateLimitFilter(int tokens, Duration refillDuration) {
        this.tokens = tokens;
        this.refillDuration = refillDuration;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to POST /login (adjust if your login-processing URL is different)
        return !("/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Bucket bucket = cache.computeIfAbsent(clientIp, k -> createNewBucket(tokens, refillDuration));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Login rate limit exceeded for IP={}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many login attempts. Try again later.\"}");
        }
    }

    private Bucket createNewBucket(int tokens, Duration refill) {
        Bandwidth limit = Bandwidth.classic(tokens, Refill.intervally(tokens, refill));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xReal = request.getHeader("X-Real-IP");
        if (xReal != null && !xReal.isBlank()) {
            return xReal;
        }
        return request.getRemoteAddr();
    }
}