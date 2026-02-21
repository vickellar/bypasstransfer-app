package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.EmailVerificationToken;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.EmailVerificationTokenRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditService auditService;

    public String createTokenForUser(User user) {
        if (user == null) return null;
        try { tokenRepository.deleteByUser(user); } catch (Exception e) { }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusDays(2);
        EmailVerificationToken evt = new EmailVerificationToken(token, user, expiry);
        tokenRepository.save(evt);

        String verifyLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/verify-email")
                .queryParam("token", token)
                .toUriString();

        try {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                Map<String, Object> model = new HashMap<>();
                model.put("username", user.getUsername());
                model.put("link", verifyLink);
                emailService.sendTemplateEmail(user.getEmail(), "Verify your email", "verify-email.html", model);
            } else {
                // fallback to console
                String body = "Hello " + user.getUsername() + ",\n\n" +
                        "Please verify your email by visiting the following link (expires in 48 hours):\n" +
                        verifyLink + "\n\n" +
                        "If you didn't create an account, please ignore this message.\n";
                emailService.sendSimpleEmail("no-reply@localhost", "Verify your email", body);
            }
        } catch (Exception ex) {
            // fallback to plain email
            try {
                String body = "Hello " + user.getUsername() + ",\n\n" +
                        "Please verify your email by visiting the following link (expires in 48 hours):\n" +
                        verifyLink + "\n\n" +
                        "If you didn't create an account, please ignore this message.\n";
                emailService.sendSimpleEmail(user.getEmail() == null ? "no-reply@localhost" : user.getEmail(), "Verify your email", body);
            } catch (Exception e) { }
        }

        try { auditService.logEntity(user.getUsername(), "users", user.getId(), "EMAIL_VERIFICATION_SENT", null, null); } catch (Exception e) { }
        return (user.getEmail() == null || user.getEmail().isBlank()) ? verifyLink : null;
    }

    public boolean validateTokenAndVerifyUser(String token) {
        if (token == null) return false;
        EmailVerificationToken evt = tokenRepository.findByToken(token);
        if (evt == null) return false;
        if (evt.getExpiry() == null || evt.getExpiry().isBefore(LocalDateTime.now())) return false;

        User user = evt.getUser();
        if (user == null) return false;

        user.setEmailVerified(true);
        userRepository.save(user);

        try { tokenRepository.delete(evt); } catch (Exception e) { }
        try { auditService.logEntity(user.getUsername(), "users", user.getId(), "EMAIL_VERIFIED", null, null); } catch (Exception e) { }
        return true;
    }
}