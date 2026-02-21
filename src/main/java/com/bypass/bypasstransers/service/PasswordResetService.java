package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.PasswordResetToken;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.PasswordResetTokenRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    /** Convenience overload for callers that don't need the reset link. */
    public String createTokenForUser(User user) {
        return createTokenForUser(user, null);
    }

    /**
     * Creates a password reset token. If user has no email, returns the reset link
     * so the controller can display it on the page; otherwise returns null.
     */
    public String createTokenForUser(User user, Object ignored) {
        if (user == null) return null;
        try { tokenRepository.deleteByUser(user); } catch (Exception e) { }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(6);
        PasswordResetToken prt = new PasswordResetToken(token, user, expiry);
        tokenRepository.save(prt);

        String resetLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/reset")
                .queryParam("token", token)
                .toUriString();

        String body = "Hello " + user.getUsername() + ",\n\n" +
                "Use the following link to reset your password (expires in 6 hours):\n" +
                resetLink + "\n\nIf you didn't request this, please ignore.\n";

        try {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                Map<String, Object> model = new HashMap<>();
                model.put("username", user.getUsername());
                model.put("link", resetLink);
                emailService.sendTemplateEmail(user.getEmail(), "Password reset", "password-reset.html", model);
            } else {
                // fallback to console if no email
                emailService.sendSimpleEmail("no-reply@localhost", "Password reset (no email)", body);
            }
        } catch (Exception ex) {
            // fallback to simple email
            try { emailService.sendSimpleEmail(user.getEmail() == null ? "no-reply@localhost" : user.getEmail(), "Password reset", body); } catch (Exception e) { }
        }

        try { auditService.logEntity(user.getUsername(), "users", user.getId(), "REQUEST_PASSWORD_RESET", null, null); } catch (Exception e) { }
        // Return link when user has no email so it can be displayed on the page
        return (user.getEmail() == null || user.getEmail().isBlank()) ? resetLink : null;
    }

    public User validateTokenAndFetchUser(String token) {
        if (token == null) return null;
        PasswordResetToken prt = tokenRepository.findByToken(token);
        if (prt == null) return null;
        if (prt.getExpiry() == null || prt.getExpiry().isBefore(LocalDateTime.now())) return null;
        return prt.getUser();
    }

    public boolean resetPassword(String token, String rawPassword) {
        if (token == null || rawPassword == null || rawPassword.isBlank()) return false;
        if (rawPassword.length() < 6) return false;
        PasswordResetToken prt = tokenRepository.findByToken(token);
        if (prt == null) return false;
        if (prt.getExpiry() == null || prt.getExpiry().isBefore(LocalDateTime.now())) return false;

        User user = prt.getUser();
        if (user == null) return false;

        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        try { tokenRepository.delete(prt); } catch (Exception e) { }
        try { auditService.logEntity(user.getUsername(), "users", user.getId(), "PASSWORD_RESET", null, null); } catch (Exception e) { }
        return true;
    }
}