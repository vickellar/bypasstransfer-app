package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventsListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventsListener.class);

    private final AuditService auditService;

    public AuthenticationEventsListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Object principal = event.getAuthentication().getPrincipal();
            log.info("AUTH SUCCESS - principal={}", principal);
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                auditService.log(username, "LOGIN_SUCCESS");
            }
        } catch (Exception ex) {
            log.warn("AUTH SUCCESS - event={} error={}", event, ex.getMessage());
        }
    }

    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        try {
            Object principal = event.getAuthentication().getPrincipal();
            String username = (principal != null) ? principal.toString() : "unknown";
            log.warn("AUTH FAILURE - principal={} - exception={}", principal, event.getException().getMessage());
            auditService.log(username, "LOGIN_FAILURE");
        } catch (Exception ex) {
            log.warn("AUTH FAILURE - event={} error={}", event, ex.getMessage());
        }
    }
}
