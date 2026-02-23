package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.PasswordResetTokenRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.AuditService;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.UserProvisioningService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserProvisioningService userProvisioningService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AccountRepository accountRepository;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                         AuditService auditService, PasswordResetService passwordResetService,
                         UserProvisioningService userProvisioningService,
                         PasswordResetTokenRepository passwordResetTokenRepository,
                         AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordResetService = passwordResetService;
        this.userProvisioningService = userProvisioningService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.accountRepository = accountRepository;
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
            // Check for duplicate username
            if (isNew) {
                List<User> existingUsers = userRepository.findByUsername(user.getUsername());
                if (!existingUsers.isEmpty()) {
                    ra.addFlashAttribute("error", "Username '" + user.getUsername() + "' is already taken. Please choose a different username.");
                    return "redirect:/users/new";
                }
            } else {
                // When editing, check if another user has the same username
                List<User> existingUsers = userRepository.findByUsername(user.getUsername());
                for (User existing : existingUsers) {
                    if (!existing.getId().equals(user.getId())) {
                        ra.addFlashAttribute("error", "Username '" + user.getUsername() + "' is already taken by another user.");
                        return "redirect:/users/edit/" + user.getId();
                    }
                }
            }
            
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            } else {
                // preserve existing password when editing and no new password provided
                if (!isNew) {
                    User existing = userRepository.findById(user.getId()).orElse(null);
                    if (existing != null) {
                        user.setPassword(existing.getPassword());
                    }
                } else {
                    // Set a default password for new users if none provided
                    user.setPassword(passwordEncoder.encode("changeme123"));
                }
            }
            
            // Save the user first to get an ID
            User savedUser = userRepository.save(user);

            // If newly created, create default wallets (Mukuru, Econet, Innbucks)
            if (isNew) {
                userProvisioningService.createDefaultWalletsForUser(savedUser);
                
                // Send password reset link if email exists
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    try {
                        passwordResetService.createTokenForUser(savedUser);
                    } catch (Exception ex) {
                        // ignore send failures but log audit
                    }
                }
            }

            // audit log
            auditService.logEntity("admin", "users", savedUser.getId(), isNew ? "CREATE_USER" : "UPDATE_USER", null, savedUser.getUsername());

            ra.addFlashAttribute("success", isNew ? 
                "User created successfully with default wallets (Mukuru, Econet, Innbucks)" : 
                "User updated successfully");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to save user: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/delete")
    @Transactional
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

            // Step 1: Delete all accounts/wallets belonging to this user first
            // This avoids foreign key constraint violation
            System.out.println("[DELETE USER] Deleting accounts for user ID: " + id);
            accountRepository.deleteByOwnerId(id);
            System.out.println("[DELETE USER] Accounts deleted successfully");
            
            // Step 2: Delete password reset tokens
            System.out.println("[DELETE USER] Deleting password reset tokens for user ID: " + id);
            passwordResetTokenRepository.deleteByUser(u);
            System.out.println("[DELETE USER] Password reset tokens deleted successfully");
            
            // Step 3: Finally delete the user
            System.out.println("[DELETE USER] Deleting user ID: " + id);
            userRepository.deleteById(id);
            System.out.println("[DELETE USER] User deleted successfully");
            
            auditService.logEntity("admin", "users", id, "DELETE_USER", null, null);
            ra.addFlashAttribute("success", "User and all associated accounts deleted");
        } catch (Exception ex) {
            System.err.println("[DELETE USER] Error deleting user ID " + id + ": " + ex.getMessage());
            ex.printStackTrace();
            ra.addFlashAttribute("error", "Failed to delete user: " + ex.getMessage());
        }
        return "redirect:/users";
    }

    @ModelAttribute("availableRoles")
    public List<Role> availableRoles() {
        return Arrays.asList(Role.values());
    }
}