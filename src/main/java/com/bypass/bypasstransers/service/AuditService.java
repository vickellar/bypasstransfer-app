package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.AuditLog;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AuditLogRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository repo;

    @Autowired
    private UserRepository userRepository;

    private Long resolveFallbackPerformerId() {
        // Prefer superadmin, otherwise first user in DB
        try {
            List<User> superAdmins = userRepository.findByUsername("superadmin");
            if (!superAdmins.isEmpty()) return superAdmins.get(0).getId();
            return userRepository.findAll().stream().findFirst().map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveCurrentUsernameIfNull(String username) {
        if (username != null && !username.isBlank()) return username;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public void log(String username, String action) {
        String resolvedUsername = resolveCurrentUsernameIfNull(username);

        AuditLog log = new AuditLog();
        log.setAction(action != null ? action : "-");
        log.setPerformedAt(LocalDateTime.now());

        // entity columns are required in schema; mark as "system" / 0 for generic logs
        log.setEntityName("system");
        log.setEntityId(0L);

        Long performerId = null;
        try {
            if (resolvedUsername != null) {
                List<User> users = userRepository.findByUsername(resolvedUsername);
                if (!users.isEmpty()) performerId = users.get(0).getId();
            }
        } catch (Exception e) {
            // ignore
        }

        if (performerId == null) {
            performerId = resolveFallbackPerformerId();
        }

        if (performerId != null) {
            log.setPerformedBy(performerId);
        } else {
            // Last resort: set to 0 to satisfy NOT NULL (may still violate FK)
            log.setPerformedBy(0L);
        }

        repo.save(log);
    }

    /**
     * More detailed audit entry when you have an entity context.
     */
    public void logEntity(String username, String entityName, Long entityId, String action, String oldValue, String newValue) {
        String resolvedUsername = resolveCurrentUsernameIfNull(username);

        AuditLog log = new AuditLog();
        log.setEntityName(entityName != null ? entityName : "system");
        log.setEntityId(entityId != null ? entityId : 0L);
        log.setAction(action != null ? action : "-" );
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setPerformedAt(LocalDateTime.now());

        Long performerId = null;
        try {
            if (resolvedUsername != null) {
                List<User> users = userRepository.findByUsername(resolvedUsername);
                if (!users.isEmpty()) performerId = users.get(0).getId();
            }
        } catch (Exception e) {
            // ignore
        }

        if (performerId == null) {
            performerId = resolveFallbackPerformerId();
        }

        if (performerId != null) {
            log.setPerformedBy(performerId);
        } else {
            log.setPerformedBy(0L);
        }

        repo.save(log);
    }
}