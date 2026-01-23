package com.bypass.bypasstransers.model;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("africas-talking")
public class AfricasTalkingSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(AfricasTalkingSmsProvider.class);

    private String username;
    private String apiKey;

    @PostConstruct
    public void init() {
        this.username = System.getenv("AT_USERNAME");
        this.apiKey = System.getenv("AT_API_KEY");

        if (username == null || apiKey == null) {
            log.warn("AfricasTalking credentials not set in environment. Provider will run in dry-run mode.");
        } else {
            log.info("AfricasTalking provider initialized (dry-run). Username present: {}", username != null);
            // If you add the AfricasTalking SDK to the project, initialize it here.
            // e.g. AfricasTalking.initialize(username, apiKey);
        }
    }

    @Override
    public void send(String phoneNumber, String message) {
        // Placeholder implementation: in production replace with SDK call.
        if (username == null || apiKey == null) {
            log.info("[DRY-RUN] SMS to {}: {}", phoneNumber, message);
        } else {
            // TODO: integrate with AfricasTalking SDK here when available
            log.info("[SIMULATED] Sending SMS via AfricasTalking to {}: {}", phoneNumber, message);
        }
    }
}