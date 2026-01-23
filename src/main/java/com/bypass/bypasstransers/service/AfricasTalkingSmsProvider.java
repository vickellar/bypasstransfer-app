
package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.SmsProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("africas-talking")
public class AfricasTalkingSmsProvider implements SmsProvider {

    @Override
    public void send(String phoneNumber, String message) {
        // Placeholder – real API can be added later
        System.out.println("[AFRICAS TALKING] SMS to " 
            + phoneNumber + ": " + message);
    }
}
