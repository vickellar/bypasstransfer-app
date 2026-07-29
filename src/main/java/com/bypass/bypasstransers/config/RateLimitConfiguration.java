package com.bypass.bypasstransers.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket loginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    @Bean
    public Bucket passwordResetBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
