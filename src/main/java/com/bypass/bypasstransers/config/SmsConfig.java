
package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.model.SmsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Default SMS provider (console/fallback) when africas-talking profile is not active.
 */
@Configuration
public class SmsConfig {

    private static final Logger log = LoggerFactory.getLogger(SmsConfig.class);

    @Bean
    @Profile("!africas-talking")
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider smsProvider() {
        return new SmsProvider() {
            @Override
            public void send(String phoneNumber, String message) {
                log.info("SMS TO {}: {}", phoneNumber, message);
            }
        };
    }
}

