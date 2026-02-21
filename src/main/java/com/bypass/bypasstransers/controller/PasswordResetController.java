package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    public PasswordResetController(UserRepository userRepository, PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
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
        User user = userRepository.findByEmailIgnoreCase(trimmed);
        if (user == null) {
            user = userRepository.findByUsernameIgnoreCase(trimmed);
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

}