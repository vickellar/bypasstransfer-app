package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.SmsProvider;
import com.bypass.bypasstransers.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Autowired
    private SmsProvider provider;

    @Autowired
    private AuditService auditService;

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
        }
    }
}