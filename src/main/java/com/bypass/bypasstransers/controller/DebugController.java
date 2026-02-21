package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.PasswordResetToken;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.repository.PasswordResetTokenRepository;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.SmsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetService passwordResetService;
    private final SmsService smsService;

    public DebugController(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepository, PasswordResetService passwordResetService, SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.passwordResetService = passwordResetService;
        this.smsService = smsService;
    }

    @GetMapping("/debug/check-password")
    public String checkPassword(@RequestParam String username, @RequestParam String raw) {
        User u = userRepository.findByUsername(username);
        if (u == null) {
            return "user-not-found";
        }
        String stored = u.getPassword();
        boolean matches = passwordEncoder.matches(raw, stored);
        return "stored=" + stored + "\nmatches=" + matches;
    }

    @GetMapping("/debug/list-users")
    public String listUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return "no-users";
        return users.stream().map(User::getUsername).collect(Collectors.joining(","));
    }

    @GetMapping("/debug/count")
    public String countUsers() {
        return "count=" + userRepository.count();
    }

    @GetMapping("/debug/raw")
    public String rawUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return "no-users";
        return users.stream().map(u -> u.getUsername() + ":" + u.getPassword()).collect(Collectors.joining("\n"));
    }

    @GetMapping("/debug/seed-defaults")
    public String seedDefaults() {
        if (userRepository.count() > 0) return "already-seeded";
        User superAdmin = new User();
        superAdmin.setUsername("superadmin");
        superAdmin.setPassword(passwordEncoder.encode("superpass"));
        superAdmin.setPhoneNumber("+1000000000");
        superAdmin.setRole(Role.SUPER_ADMIN);
        userRepository.save(superAdmin);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setPhoneNumber("+1000000001");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User staff = new User();
        staff.setUsername("staff");
        staff.setPassword(passwordEncoder.encode("staffpass"));
        staff.setPhoneNumber("+1000000002");
        staff.setRole(Role.STAFF);
        userRepository.save(staff);

        return "seeded";
    }

    @GetMapping("/debug/migrate-passwords")
    public String migratePasswords() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return "no-users";

        List<String> updated = users.stream().filter(u -> {
            String pw = u.getPassword();
            return pw != null && !pw.startsWith("$2"); // not BCrypt-hashed
        }).map(u -> {
            String raw = u.getPassword();
            // encode and update
            u.setPassword(passwordEncoder.encode(raw));
            userRepository.save(u);
            return u.getUsername();
        }).collect(Collectors.toList());

        return "updatedCount=" + updated.size() + "\nusers=" + String.join(",", updated);
    }

    @GetMapping("/debug/reset-password")
    public String resetPassword(@RequestParam String username, @RequestParam String newPassword) {
        User u = userRepository.findByUsername(username);
        if (u == null) return "user-not-found";
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        return "reset-ok";
    }

    @GetMapping("/debug/users")
    public String usersWithRoles() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return "no-users";
        return users.stream()
                .map(u -> u.getUsername() + ":" + (u.getRole() == null ? "null" : u.getRole().name()))
                .collect(Collectors.joining("\n"));
    }

    @GetMapping("/debug/tokens")
    public String listTokens() {
        List<PasswordResetToken> tokens = tokenRepository.findAll();
        if (tokens.isEmpty()) return "no-tokens";
        return tokens.stream()
                .map(t -> t.getToken() + ":" + (t.getUser() == null ? "null" : t.getUser().getUsername()) + ":" + t.getExpiry())
                .collect(Collectors.joining("\n"));
    }

    @GetMapping("/debug/create-token")
    public String createTokenForUser(@RequestParam String username) {
        User u = userRepository.findByUsername(username);
        if (u == null) return "user-not-found";
        String link = passwordResetService.createTokenForUser(u);
        return link != null ? link : "Token created (email sent - check console for link)";
    }

    @GetMapping("/debug/send-sms")
    public String debugSendSms(@RequestParam String phone, @RequestParam String message) {
        try {
            smsService.send(phone, message);
            return "sent";
        } catch (Exception ex) {
            return "error:" + ex.getMessage();
        }
    }

    @GetMapping("/debug/validate-token")
    public String validateToken(@RequestParam String token) {
        if (token == null || token.isBlank()) return "token-required";
        User u = passwordResetService.validateTokenAndFetchUser(token);
        if (u == null) return "invalid-or-expired";
        return "valid:" + u.getUsername();
    }

    @GetMapping("/debug/reset-by-token")
    public String resetByToken(@RequestParam String token, @RequestParam String newPassword) {
        if (token == null || token.isBlank()) return "token-required";
        if (newPassword == null || newPassword.isBlank()) return "password-required";
        boolean ok = passwordResetService.resetPassword(token, newPassword);
        return ok ? "reset-ok" : "reset-failed";
    }

    @GetMapping("/debug/set-role")
    public String setRole(@RequestParam String username, @RequestParam String role) {
        User u = userRepository.findByUsername(username);
        if (u == null) return "user-not-found";
        try {
            u.setRole(com.bypass.bypasstransers.enums.Role.valueOf(role));
        } catch (IllegalArgumentException ex) {
            return "invalid-role";
        }
        userRepository.save(u);
        return "role-set:" + username + ":" + role;
    }

}