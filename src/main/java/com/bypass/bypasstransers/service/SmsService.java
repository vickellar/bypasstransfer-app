package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.SmsProvider;
import com.bypass.bypasstransers.model.User;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final SmsProvider provider;
    private final AuditService auditService;

    public SmsService(SmsProvider provider, AuditService auditService) {
        this.provider = provider;
        this.auditService = auditService;
    }

    public void send(String phoneNumber, String message) {
        // Delegate to provider (placeholder implementations may just print)
        try {
            provider.send(phoneNumber, message);
        } catch (Exception e) {
            // fallback: print
            System.out.println("SMS TO " + phoneNumber + ": " + message);
        }
    }

    public void sendAlert(User user, String message) {
        if (user == null) return;
        send(user.getPhoneNumber(), message);

        try {
            auditService.log(
                user.getUsername(),
                "SMS sent: " + message
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}