package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.ContactMessage;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.ContactMessageRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.EmailVerificationService;
import com.bypass.bypasstransers.service.EmailService;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.SmsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class PublicController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final ContactMessageRepository contactMessageRepository;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final SmsService smsService;

    public PublicController(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            PasswordResetService passwordResetService,
                            ContactMessageRepository contactMessageRepository,
                            EmailVerificationService emailVerificationService,
                            EmailService emailService,
                            SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.contactMessageRepository = contactMessageRepository;
        this.emailVerificationService = emailVerificationService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // simple links will be rendered by the Thymeleaf template
        return "frontpage";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerPost(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam(required = false) String phoneNumber,
                               @RequestParam(required = false) String password,
                               RedirectAttributes ra) {
        // basic duplicate checks
        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            ra.addFlashAttribute("error", "Username and email are required");
            return "redirect:/register";
        }

        if (userRepository.findByUsernameIgnoreCase(username) != null) {
            ra.addFlashAttribute("error", "Username already exists");
            return "redirect:/register";
        }
        if (userRepository.findByEmailIgnoreCase(email) != null) {
            ra.addFlashAttribute("error", "An account with that email already exists");
            return "redirect:/register";
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhoneNumber(phoneNumber);
        u.setRole(Role.STAFF); // default role for self-registered users

        boolean sentResetLink = false;

        if (password != null && !password.isBlank()) {
            u.setPassword(passwordEncoder.encode(password));
            userRepository.save(u);
        } else {
            // generate a temporary password, save encoded, then create a reset token and email it
            String temp = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
            u.setPassword(passwordEncoder.encode(temp));
            userRepository.save(u);
            try {
                String link = passwordResetService.createTokenForUser(u);
                // if no email on user, createTokenForUser returns link so we can show it
                sentResetLink = (link != null);
                // otherwise emailService will have sent the link to user's email
            } catch (Exception ex) {
                // ignore but continue
            }
        }

        // Send email verification token (if email present) so users can verify their address
        try {
            String verifyLink = emailVerificationService.createTokenForUser(u);
            if (verifyLink != null) {
                // no email available — the service returned the link which we can show in logs
                // we won't display it in UI for security; it will be printed to logs by EmailService fallback
            }
        } catch (Exception e) {
            // ignore verification send failures
        }

        if (sentResetLink) {
            ra.addFlashAttribute("success", "Registration successful. Use the reset link shown in logs to set your password.");
        } else {
            ra.addFlashAttribute("success", "Registration successful. Please check your email to set your password if you didn't provide one.");
        }
        return "redirect:/login";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @PostMapping("/contact")
    public String contactPost(@RequestParam String name,
                              @RequestParam String email,
                              @RequestParam String message,
                              RedirectAttributes ra) {
        // For now, persist the message and acknowledge.
        if (name == null || name.isBlank() || email == null || email.isBlank() || message == null || message.isBlank()) {
            ra.addFlashAttribute("error", "Please provide name, email and message");
            return "redirect:/contact";
        }

        ContactMessage cm = new ContactMessage(name, email, message);
        try {
            contactMessageRepository.save(cm);

            // Notify admins by email and SMS
            List<User> admins = userRepository.findAll();
            for (User u : admins) {
                if (u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN || u.getRole() == Role.SUPERVISOR) {
                    // send email if admin has email
                    if (u.getEmail() != null && !u.getEmail().isBlank()) {
                        Map<String, Object> model = new HashMap<>();
                        model.put("name", name);
                        model.put("email", email);
                        model.put("message", message);
                        model.put("adminName", u.getUsername());
                        emailService.sendTemplateEmail(u.getEmail(), "New contact message", "contact-notification.html", model);
                    }
                    // send SMS if admin has phone
                    if (u.getPhoneNumber() != null && !u.getPhoneNumber().isBlank()) {
                        try {
                            smsService.sendAlert(u, "New contact message from " + name + ": " + (message.length() > 120 ? message.substring(0, 120) + "..." : message));
                        } catch (Exception ex) {
                            // ignore sms failures
                        }
                    }
                }
            }

            ra.addFlashAttribute("success", "Thank you for contacting us. We'll get back to you shortly.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to submit message: " + ex.getMessage());
        }
        return "redirect:/contact";
    }
}