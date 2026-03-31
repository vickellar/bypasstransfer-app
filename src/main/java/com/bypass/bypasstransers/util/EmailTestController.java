package com.bypass.bypasstransers.util;

import com.bypass.bypasstransers.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    // Use the configured sender account as the default test recipient.
    // This matches what EmailService uses as the `From:` address.
    @Value("${spring.mail.username:}")
    private String defaultTo;

    @GetMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam(required = false) String to) {
        String recipient = (to != null && !to.isBlank()) ? to : defaultTo;
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.status(400).body(
                    "Missing recipient. Provide ?to=... or set spring.mail.username (MAIL_USERNAME).");
        }
        boolean ok = emailService.sendSimpleEmail(
                recipient,
                "Test Email from ByPass Transfers",
                "This is a test email to confirm that email functionality is working properly.");
        if (ok) {
            return ResponseEntity.ok("SMTP accepted the message. Check the recipient inbox (and spam).");
        }
        return ResponseEntity.status(503).body(
                "SMTP did not send (or mail is not configured). Check server logs; set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD.");
    }

    @GetMapping("/send-template-email")
    public ResponseEntity<String> sendTemplateEmail(@RequestParam(required = false) String to) {
        String recipient = (to != null && !to.isBlank()) ? to : defaultTo;
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.status(400).body(
                    "Missing recipient. Provide ?to=... or set spring.mail.username (MAIL_USERNAME).");
        }
        Map<String, Object> model = new HashMap<>();
        model.put("username", "Test User");
        model.put("link", "https://example.com/reset-password");

        boolean ok = emailService.sendTemplateEmail(
                recipient,
                "Template Test Email",
                "password-reset.html",
                model);
        if (ok) {
            return ResponseEntity.ok("SMTP accepted the template message. Check the recipient inbox.");
        }
        return ResponseEntity.status(503).body(
                "SMTP did not send (or mail is not configured). Check server logs; set MAIL_* / spring.mail.*.");
    }
}