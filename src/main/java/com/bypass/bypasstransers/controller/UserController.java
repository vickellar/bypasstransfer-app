package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.AuditService;
import com.bypass.bypasstransers.service.PasswordResetService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordResetService passwordResetService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService, PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model, @RequestParam(value = "role", required = false) Role preRole) {
        User u = new User();
        if (preRole != null) {
            u.setRole(preRole);
        }
        model.addAttribute("user", u);
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        model.addAttribute("user", u.get());
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) String rawPassword, RedirectAttributes ra) {
        boolean isNew = (user.getId() == null);
        try {
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            } else {
                // preserve existing password when editing and no new password provided
                if (!isNew) {
                    User existing = userRepository.findById(user.getId()).orElse(null);
                    if (existing != null) {
                        user.setPassword(existing.getPassword());
                    }
                }
            }
            userRepository.save(user);

            // If newly created and no password provided, send password reset link if email exists
            if (isNew && (rawPassword == null || rawPassword.isBlank()) && user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    passwordResetService.createTokenForUser(user);
                } catch (Exception ex) {
                    // ignore send failures but log audit
                }
            }

            // audit log
            auditService.logEntity("admin", "users", user.getId(), isNew ? "CREATE_USER" : "UPDATE_USER", null, user.getUsername());

            ra.addFlashAttribute("success", "User saved");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to save user: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam Long id, RedirectAttributes ra) {
        try {
            Optional<User> opt = userRepository.findById(id);
            if (opt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            }
            User u = opt.get();

            // Prevent deleting the last admin-capable account
            long adminCount = userRepository.countByRole(Role.ADMIN);
            long superCount = userRepository.countByRole(Role.SUPER_ADMIN);
            if (u.getRole() == Role.SUPER_ADMIN) {
                if (superCount <= 1 && adminCount == 0) {
                    ra.addFlashAttribute("error", "Cannot delete the last administrative user. Add another admin first.");
                    return "redirect:/users";
                }
            } else if (u.getRole() == Role.ADMIN) {
                if (adminCount <= 1 && superCount == 0) {
                    ra.addFlashAttribute("error", "Cannot delete the last administrative user. Add another admin first.");
                    return "redirect:/users";
                }
            }

            userRepository.deleteById(id);
            auditService.logEntity("admin", "users", id, "DELETE_USER", null, null);
            ra.addFlashAttribute("success", "User deleted");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to delete user: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @ModelAttribute("availableRoles")
    public List<Role> availableRoles() {
        return Arrays.asList(Role.values());
    }
}