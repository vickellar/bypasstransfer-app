package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.ContactMessage;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.ContactMessageRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.EmailVerificationService;
import com.bypass.bypasstransers.service.EmailService;
import com.bypass.bypasstransers.service.ExchangeRateService;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.SmsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for public access routes like home, about, registration, and contact.
 */
@Controller
public class PublicController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final ContactMessageRepository contactMessageRepository;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ExchangeRateService exchangeRateService;

    public PublicController(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetService passwordResetService,
            ContactMessageRepository contactMessageRepository,
            EmailVerificationService emailVerificationService,
            EmailService emailService,
            SmsService smsService,
            ExchangeRateService exchangeRateService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.contactMessageRepository = contactMessageRepository;
        this.emailVerificationService = emailVerificationService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Check if user is authenticated using SecurityContextHolder
        org.springframework.security.core.context.SecurityContext context = org.springframework.security.core.context.SecurityContextHolder
                .getContext();
        org.springframework.security.core.Authentication authentication = context.getAuthentication();

        // If user is logged in (not anonymous), redirect to their dashboard
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return "redirect:/app";
        }

        // Add exchange rates to the model for the frontpage
        model.addAttribute("exchangeRates", exchangeRateService.getAllRates());

        // Otherwise show the landing page
        return "frontpage";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/register")
    public String register(jakarta.servlet.http.HttpServletRequest request) {
        // Ensure session exists early to prevent "Session committed" errors during CSRF processing
        request.getSession(true);
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

        if (!userRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            ra.addFlashAttribute("error", "Username already exists");
            return "redirect:/register";
        }
        if (!userRepository.findByEmailIgnoreCase(email).isEmpty()) {
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
            // generate a temporary password, save encoded, then create a reset token and
            // email it
            String temp = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
            u.setPassword(passwordEncoder.encode(temp));
            userRepository.save(u);

            try {
                var prOut = passwordResetService.createTokenForUser(u);
                sentResetLink = (prOut.getDisplayLinkOptional() != null);
            } catch (Exception ex) {
                // ignore but continue
            }
        }

        // Send email verification token (if email present) so users can verify their address
        try {
            emailVerificationService.createTokenForUser(u);
        } catch (Exception e) {
            // ignore verification send failures
        }

        if (sentResetLink) {
            ra.addFlashAttribute("success",
                    "Registration successful. Use the reset link shown in logs to set your password.");
        } else {
            ra.addFlashAttribute("success",
                    "Registration successful. Please check your email to set your password if you didn't provide one.");
        }
        return "redirect:/login";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        if (!model.containsAttribute("contactMessage")) {
            model.addAttribute("contactMessage", new ContactMessage());
        }
        return "contact";
    }

    @PostMapping("/contact")
    public String contactPost(@ModelAttribute("contactMessage") ContactMessage contactMessage,
            @RequestParam(required = false) Boolean newsletter,
            RedirectAttributes ra) {

        if (contactMessage.getName() == null || contactMessage.getName().isBlank() ||
                contactMessage.getEmail() == null || contactMessage.getEmail().isBlank() ||
                contactMessage.getMessage() == null || contactMessage.getMessage().isBlank()) {
            ra.addFlashAttribute("error", "Please provide name, email and message");
            ra.addFlashAttribute("contactMessage", contactMessage);
            return "redirect:/contact";
        }

        try {
            contactMessageRepository.save(contactMessage);

            // Notify admins - basic logic
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN
                            || u.getRole() == Role.SUPERVISOR)
                    .forEach(u -> {
                        // Send Email
                        if (u.getEmail() != null && !u.getEmail().isBlank()) {
                            try {
                                Map<String, Object> emailModel = new HashMap<>();
                                emailModel.put("name", contactMessage.getName());
                                emailModel.put("email", contactMessage.getEmail());
                                emailModel.put("subject",
                                        contactMessage.getSubject() != null ? contactMessage.getSubject()
                                                : "General Inquiry");
                                emailModel.put("message", contactMessage.getMessage());
                                emailModel.put("adminName", u.getUsername());
                                emailService.sendTemplateEmail(u.getEmail(), "New contact message",
                                        "contact-notification.html", emailModel);
                            } catch (Exception e) {
                            }
                        }
                        // Send SMS
                        if (u.getPhoneNumber() != null && !u.getPhoneNumber().isBlank()) {
                            try {
                                smsService.sendAlert(u, "New msg from " + contactMessage.getName());
                            } catch (Exception e) {
                            }
                        }
                    });

            ra.addFlashAttribute("success", "Thank you for contacting us. We'll get back to you shortly.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to submit message: " + ex.getMessage());
            ra.addFlashAttribute("contactMessage", contactMessage);
        }
        return "redirect:/contact";
    }
}