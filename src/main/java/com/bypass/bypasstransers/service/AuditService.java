package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.AuditLog;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AuditLogRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repo;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

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

    /**
     * Get client IP address from request headers
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();

                // Check for IP forwarded by proxy
                String xForwarded = request.getHeader("X-Forwarded-For");
                if (xForwarded != null && !xForwarded.isEmpty()) {
                    return xForwarded.split(",")[0].trim();
                }

                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }

                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.debug("Could not determine client IP", e);
        }
        return "UNKNOWN";
    }

    /**
     * Log a security event with full context
     */
    public void logSecurityEvent(String action, String details, boolean success) {
        try {
            String username = getCurrentUsername();
            String ipAddress = getClientIpAddress();
            Long userId = getCurrentUserId();

            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityName("security_event");
            log.setEntityId(userId != null ? userId : 0L);
            log.setOldValue(ipAddress); // Store IP for security tracking
            log.setNewValue(success ? "SUCCESS" : "FAILURE");
            log.setPerformedAt(LocalDateTime.now());
            log.setPerformedBy(userId);

            repo.save(log);

            logger.warn("SECURITY_AUDIT [{}] user={}, ip={}, action={}, success={}, details={}",
                    LocalDateTime.now(), username, ipAddress, action, success, details);
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        }
    }

    /**
     * Log failed login attempt
     */
    public void logFailedLogin(String username) {
        String ipAddress = getClientIpAddress();
        logSecurityEvent("LOGIN_FAILURE", "Failed login attempt for: " + username + " from " + ipAddress, false);
    }

    /**
     * Log successful login
     */
    public void logSuccessfulLogin(String username) {
        String ipAddress = getClientIpAddress();
        logSecurityEvent("LOGIN_SUCCESS", "Successful login for: " + username + " from " + ipAddress, true);
    }

    /**
     * Log unauthorized access attempt
     */
    public void logUnauthorizedAccess(String resource, String username) {
        logSecurityEvent("UNAUTHORIZED_ACCESS", "Attempted to access: " + resource, false);
    }

    /**
     * Log data modification
     */
    public void logDataModification(String entityName, Long entityId, String action, String oldValue, String newValue) {
        String username = getCurrentUsername();
        logSecurityEvent(action, "Modified " + entityName + " #" + entityId, true);

        try {
            Long userId = getCurrentUserId();
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityName(entityName);
            log.setEntityId(entityId != null ? entityId : 0L);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setPerformedAt(LocalDateTime.now());
            log.setPerformedBy(userId);
            repo.save(log);
        } catch (Exception e) {
            logger.error("Failed to log data modification", e);
        }
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
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
            logger.debug("Could not determine current username", e);
        }
        return "ANONYMOUS";
    }

    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (!"ANONYMOUS".equals(username)) {
                List<User> users = userRepository.findByUsername(username);
                return users.isEmpty() ? null : users.get(0).getId();
            }
        } catch (Exception e) {
            logger.debug("Could not determine current user ID", e);
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