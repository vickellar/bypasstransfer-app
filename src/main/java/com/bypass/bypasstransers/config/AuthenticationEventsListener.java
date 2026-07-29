package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventsListener {

    @Autowired(required = false)
    private AuditService enhancedAuditService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            if (enhancedAuditService != null) {
                enhancedAuditService.logSuccessfulLogin(user.getUsername());
            }
        }
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        if (event.getAuthentication() != null) {
            String username = event.getAuthentication().getName();
            if (enhancedAuditService != null) {
                enhancedAuditService.logFailedLogin(username);
            }
        }
    }
}