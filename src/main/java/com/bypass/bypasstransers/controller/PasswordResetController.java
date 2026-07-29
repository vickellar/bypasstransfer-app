package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.EmailSendOutcome;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.AuditService;
import com.bypass.bypasstransers.service.PasswordStrengthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordStrengthService passwordStrengthService;
    private final AuditService enhancedAuditService;

    public PasswordResetController(UserRepository userRepository,
                                   PasswordResetService passwordResetService,
                                   PasswordEncoder passwordEncoder,
                                   AuditService auditService,
                                   PasswordStrengthService passwordStrengthService,
                                   AuditService enhancedAuditService) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordStrengthService = passwordStrengthService;
        this.enhancedAuditService = enhancedAuditService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam(required = false) String emailOrUsername,
            @RequestParam(required = false) String email,
            RedirectAttributes ra) {

        String value = (emailOrUsername != null && !emailOrUsername.isBlank()) ? emailOrUsername : email;
        if (value == null || value.isBlank()) {
            ra.addFlashAttribute("error", "Please enter your email or username.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_ATTEMPT", "Empty email/username", false);
            return "redirect:/forgot-password";
        }

        String trimmed = value.trim();

        // Sanitize input to prevent injection
        if (!isValidEmailOrUsername(trimmed)) {
            ra.addFlashAttribute("error", "Invalid email or username format.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_ATTEMPT", "Invalid format: " + trimmed, false);
            return "redirect:/forgot-password";
        }

        List<User> emailUsers = userRepository.findByEmailIgnoreCase(trimmed);
        User user = emailUsers.isEmpty() ? null : emailUsers.get(0);

        if (user == null) {
            List<User> usernameUsers = userRepository.findByUsernameIgnoreCase(trimmed);
            user = usernameUsers.isEmpty() ? null : usernameUsers.get(0);
        }

        if (user == null) {
            // Don't reveal if user exists (security best practice)
            ra.addFlashAttribute("success", "If an account matches that email or username, you will receive a password reset link.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_ATTEMPT", "User not found: " + trimmed, false);
            return "redirect:/forgot-password";
        }

        EmailSendOutcome outcome = passwordResetService.createTokenForUser(user, null);

        if (outcome.getDisplayLinkOptional() != null) {
            ra.addFlashAttribute("success", "Reset link created (no email on file - use the link below).");
            ra.addFlashAttribute("resetLink", outcome.getDisplayLinkOptional());
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_TOKEN_GENERATED", "User: " + user.getUsername(), true);
        } else if (outcome.isSmtpSent()) {
            ra.addFlashAttribute("success", "Check your email for a password reset link (also check spam).");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_EMAIL_SENT", "User: " + user.getUsername(), true);
        } else {
            ra.addFlashAttribute("error",
                    "We could not send the reset email. Please try again later or contact support.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_EMAIL_FAILED", "User: " + user.getUsername(), false);
        }

        return "redirect:/forgot-password";
    }

    @GetMapping("/reset")
    public String resetForm(@RequestParam(required = false) String token,
                            Model model, RedirectAttributes ra) {
        if (token == null || passwordResetService.validateTokenAndFetchUser(token) == null) {
            ra.addFlashAttribute("error", "Invalid or expired reset token.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_INVALID_TOKEN", "Token validation failed", false);
            return "redirect:/login";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset")
    public String handleReset(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String password,
            RedirectAttributes ra) {

        if (token == null || token.isBlank()) {
            ra.addFlashAttribute("error", "Reset token is missing. Please request a new link.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET", "Missing token", false);
            return "redirect:/forgot-password";
        }

        // Validate password strength
        PasswordStrengthService.PasswordValidationResult validation =
                passwordStrengthService.validate(password);

        if (!validation.isValid()) {
            ra.addFlashAttribute("error", "Password does not meet requirements: " + validation.getErrorsAsString());
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET", "Weak password provided", false);
            return "redirect:/reset?token=" + token;
        }

        boolean ok = passwordResetService.resetPassword(token, password);

        if (!ok) {
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET", "Token validation failed", false);
            return "redirect:/login?reset=error";
        }

        enhancedAuditService.logSecurityEvent("PASSWORD_RESET_SUCCESS", "Password reset completed", true);
        return "redirect:/login?reset=success";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes ra) {

        if (authentication == null || !authentication.isAuthenticated()) {
            ra.addFlashAttribute("error", "You must be logged in to change your password.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Not authenticated", false);
            return "redirect:/login";
        }

        String username = authentication.getName();
        List<User> users = userRepository.findByUsernameIgnoreCase(username);
        User user = users.isEmpty() ? null : users.get(0);

        if (user == null) {
            ra.addFlashAttribute("error", "User not found.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "User not found: " + username, false);
            return "redirect:/app";
        }

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Wrong current password", false);
            return "redirect:/change-password";
        }

        // Validate new password strength
        PasswordStrengthService.PasswordValidationResult validation =
                passwordStrengthService.validate(newPassword);

        if (!validation.isValid()) {
            ra.addFlashAttribute("error", "New password does not meet requirements: " + validation.getErrorsAsString());
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Weak password", false);
            return "redirect:/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New password and confirmation do not match.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Password mismatch", false);
            return "redirect:/change-password";
        }

        // Prevent password reuse
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "You cannot reuse your previous password.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Password reuse attempt", false);
            return "redirect:/change-password";
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Audit log
        try {
            auditService.logEntity(username, "users", user.getId(), "PASSWORD_CHANGE", "[REDACTED]", "[REDACTED]");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGED", "User: " + username, true);
        } catch (Exception e) {
            // Log to system logger but continue
        }

        ra.addFlashAttribute("success", "Password changed successfully. You will need to log in again.");
        return "redirect:/login";
    }

    /**
     * Validate email or username format to prevent injection
     */
    private boolean isValidEmailOrUsername(String value) {
        // Check length
        if (value == null || value.length() > 255 || value.length() < 2) {
            return false;
        }

        // Email validation (RFC 5322 simplified)
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        // Username validation
        String usernamePattern = "^[A-Za-z0-9_.-]+$";

        return value.matches(emailPattern) || value.matches(usernamePattern);
    }
}