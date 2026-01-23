
package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.model.SmsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmsConfig {

    @Bean
    public SmsProvider smsProvider() {
        return new SmsProvider() {
            @Override
            public void send(String phoneNumber, String message) {
                System.out.println("SMS TO " + phoneNumber + ": " + message);
            }
        };
    }
}

