package com.bypass.bypasstransers.util;

import com.bypass.bypasstransers.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail() {
        try {
            // Send a simple test email
            emailService.sendSimpleEmail(
                "vickellar.01@gmail.com",
                "Test Email from ByPass Transfers",
                "This is a test email to confirm that email functionality is working properly."
            );
            
            return ResponseEntity.ok("Test email sent successfully! Check your inbox (and spam folder).");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send email: " + e.getMessage());
        }
    }

    @GetMapping("/send-template-email")
    public ResponseEntity<String> sendTemplateEmail() {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("username", "Test User");
            model.put("link", "https://example.com/reset-password");
            
            emailService.sendTemplateEmail(
                "vickellar.01@gmail.com",
                "Template Test Email",
                "password-reset.html", // Using existing template
                model
            );
            
            return ResponseEntity.ok("Template email sent successfully! Check your inbox (and spam folder).");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send template email: " + e.getMessage());
        }
    }
}