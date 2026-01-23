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
    public String handleForgotPassword(@RequestParam String email, RedirectAttributes ra) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            ra.addFlashAttribute("error", "If an account with that email exists, a reset link was sent.");
            return "redirect:/forgot-password";
        }

        passwordResetService.createTokenForUser(user);
        ra.addFlashAttribute("success", "If an account with that email exists, a reset link was sent.");
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
    public String handleReset(@RequestParam String token, @RequestParam String password, RedirectAttributes ra) {
        boolean ok = passwordResetService.resetPassword(token, password);
        if (!ok) {
            ra.addFlashAttribute("error", "Reset failed or token expired.");
            return "redirect:/login";
        }
        ra.addFlashAttribute("success", "Password reset successful. Please login.");
        return "redirect:/login";
    }
}
