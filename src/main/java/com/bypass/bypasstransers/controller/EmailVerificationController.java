package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.service.EmailVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, RedirectAttributes ra) {
        if (token == null || token.isBlank()) {
            ra.addFlashAttribute("error", "Verification token is missing.");
            return "redirect:/login";
        }
        boolean ok = emailVerificationService.validateTokenAndVerifyUser(token);
        if (ok) {
            ra.addFlashAttribute("success", "Email verified. You can now login.");
        } else {
            ra.addFlashAttribute("error", "Verification token invalid or expired.");
        }
        return "redirect:/login";
    }
}
