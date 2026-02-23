package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.AuditService;
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

    public PasswordResetController(UserRepository userRepository, PasswordResetService passwordResetService,
                                   PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
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
            return "redirect:/forgot-password";
        }
        String trimmed = value.trim();
        // Look up by email first, then by username (many default users have no email)
        List<User> emailUsers = userRepository.findByEmailIgnoreCase(trimmed);
        User user = emailUsers.isEmpty() ? null : emailUsers.get(0);
        if (user == null) {
            List<User> usernameUsers = userRepository.findByUsernameIgnoreCase(trimmed);
            user = usernameUsers.isEmpty() ? null : usernameUsers.get(0);
        }
        if (user == null) {
            ra.addFlashAttribute("error", "No account found with that email or username.");
            return "redirect:/forgot-password";
        }

        String resetLink = passwordResetService.createTokenForUser(user, null);
        ra.addFlashAttribute("success", resetLink != null
                ? "Reset link created (no email on file - use the link below)."
                : "If an account with that email exists, a reset link was sent.");
        if (resetLink != null) {
            ra.addFlashAttribute("resetLink", resetLink);
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset")
    public String resetForm(@RequestParam(required = false) String token, Model model, RedirectAttributes ra) {
        if (token == null || passwordResetService.validateTokenAndFetchUser(token) == null) {
            ra.addFlashAttribute("error", "Invalid or expired reset token.");
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
            return "redirect:/forgot-password";
        }
        boolean ok = passwordResetService.resetPassword(token, password != null ? password : "");
        if (!ok) {
            // Use query param to surface the error reliably on the login page
            return "redirect:/login?reset=error";
        }
        // Use query param so the login page can show a stable success message
        return "redirect:/login?reset=success";
    }

    // Change password for logged-in users
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
            return "redirect:/login";
        }

        String username = authentication.getName();
        List<User> users = userRepository.findByUsernameIgnoreCase(username);
        User user = users.isEmpty() ? null : users.get(0);
        
        if (user == null) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/app";
        }

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/change-password";
        }

        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "New password must be at least 6 characters long.");
            return "redirect:/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New password and confirmation do not match.");
            return "redirect:/change-password";
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Audit log
        try {
            auditService.logEntity(username, "users", user.getId(), "PASSWORD_CHANGE", null, null);
        } catch (Exception e) {
            // ignore audit failures
        }

        ra.addFlashAttribute("success", "Password changed successfully.");
        return "redirect:/app";
    }

}