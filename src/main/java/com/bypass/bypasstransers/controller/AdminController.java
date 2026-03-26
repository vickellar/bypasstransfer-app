package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.service.BackupService;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.service.UserProvisioningService;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityService securityService;
    private final UserProvisioningService userProvisioningService;
    private final PasswordEncoder passwordEncoder;
    private final BackupService backupService;

    public AdminController(UserRepository userRepository, WalletRepository walletRepository, 
                          TransactionRepository transactionRepository, SecurityService securityService,
                          UserProvisioningService userProvisioningService,
                          PasswordEncoder passwordEncoder,
                          BackupService backupService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.securityService = securityService;
        this.userProvisioningService = userProvisioningService;
        this.passwordEncoder = passwordEncoder;
        this.backupService = backupService;
    }

    @GetMapping({"/", ""})
    public String adminHome(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        // Count users by role
        long totalUsers = userRepository.count();
        long staffCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.STAFF).count();
        long supervisorCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.SUPERVISOR).count();
        long adminCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN).count();
        
        // Get all wallets
        List<Wallet> allWallets = walletRepository.findAll();
        double totalBalance = allWallets.stream().mapToDouble(Wallet::getBalance).sum();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("staffCount", staffCount);
        model.addAttribute("supervisorCount", supervisorCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("totalWallets", allWallets.size());
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        
        return "admin";
    }
    
    @GetMapping("/profit")  // This maps to /admin/profit due to the @RequestMapping("/admin") on the class
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public String viewProfit(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null || !securityService.isSuperAdmin()) {
            return "redirect:/admin";
        }
        
        // Calculate total profit
        double totalProfit = transactionRepository.findAll()
            .stream()
            .filter(t -> t.getType() != null && t.getType() != TransactionType.INCOME)
            .mapToDouble(t -> t.getAmount() * ChargeCalculator.BASE_PROFIT_DEFAULT)
            .sum();
            
        long totalTransactions = transactionRepository.count();
            
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("user", currentUser);
        
        return "admin-profit";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','SUPER_ADMIN')")
    public String reportsPage(Model model) {
        // Calculate transaction statistics
        List<Transaction> allTransactions = transactionRepository.findAll();
        double totalAmount = allTransactions.stream().mapToDouble(Transaction::getAmount).sum();
        double totalFees = allTransactions.stream().mapToDouble(Transaction::getFee).sum();
        double totalNet = allTransactions.stream().mapToDouble(Transaction::getNetAmount).sum();
        
        model.addAttribute("totalTransactions", allTransactions.size());
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("totalFees", totalFees);
        model.addAttribute("totalNet", totalNet);
        
        List<User> staffMembers = userRepository.findAll().stream()
                .filter(u -> u.getIsActive() && (u.getRole() == Role.STAFF || u.getRole() == Role.SUPERVISOR))
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("staffMembers", staffMembers);
        
        return "reports";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String adminUsersPage() {
        return "admin-users";
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String adminAccountsPage(Model model) {
        model.addAttribute("allWallets", walletRepository.findAll());
        return "admin-accounts";
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String adminSettingsPage() {
        return "admin-settings";
    }

    /**
     * Migrate old users by creating default wallets (Mukuru, Econet, Innbucks) for users who don't have them.
     * This is useful for users created before the multi-account system was implemented.
     */
    @PostMapping("/migrate-users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String migrateOldUsers(RedirectAttributes ra) {
        List<User> allUsers = userRepository.findAll();
        int migratedCount = 0;
        int skippedCount = 0;
        
        for (User user : allUsers) {
            try {
                // Check if user already has default wallets
                if (!userProvisioningService.hasDefaultWallets(user)) {
                    userProvisioningService.createDefaultWalletsForUser(user);
                    migratedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                // Log error but continue with other users
                System.err.println("Failed to migrate user " + user.getUsername() + ": " + e.getMessage());
            }
        }
        
        ra.addFlashAttribute("success", "Migration complete: " + migratedCount + " users migrated, " + skippedCount + " users already had wallets.");
        return "redirect:/admin";
    }

    /**
     * Create default wallets for a specific user (admin action)
     */
    @PostMapping("/create-wallets")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String createWalletsForUser(@RequestParam Long userId, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/users";
        }
        
        User user = userOpt.get();
        try {
            if (userProvisioningService.hasDefaultWallets(user)) {
                ra.addFlashAttribute("info", "User " + user.getUsername() + " already has all default wallets.");
            } else {
                userProvisioningService.createDefaultWalletsForUser(user);
                ra.addFlashAttribute("success", "Default wallets (Mukuru, Econet, Innbucks) created for " + user.getUsername());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create wallets: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    /**
     * Activate a user and create default wallets for them
     */
    @PostMapping("/activate-user")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String activateUser(@RequestParam Long userId, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/users";
        }
        
        User user = userOpt.get();
        try {
            // Activate the user
            user.setIsActive(true);
            user.setDeletedAt(null); // Clear the deletion timestamp
            userRepository.save(user);
            
            // Create default wallets for the activated user
            userProvisioningService.createDefaultWalletsForUser(user);
            
            ra.addFlashAttribute("success", "User " + user.getUsername() + " has been activated and default wallets created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to activate user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    @GetMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String changePasswordForm(Model model) {
        model.addAttribute("user", securityService.getCurrentUser());
        return "admin-change-password";
    }
    
    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String changePassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            Optional<User> userOpt = userRepository.findById(currentUser.getId());
            
            if (userOpt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/admin/change-password";
            }
            
            User user = userOpt.get();
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                ra.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/admin/change-password";
            }
            
            // Check if new passwords match
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "New passwords do not match");
                return "redirect:/admin/change-password";
            }
            
            // Check password strength
            if (newPassword.length() < 8) {
                ra.addFlashAttribute("error", "Password must be at least 8 characters long");
                return "redirect:/admin/change-password";
            }
            
            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            ra.addFlashAttribute("success", "Password changed successfully");
            return "redirect:/admin";
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to change password: " + e.getMessage());
            return "redirect:/admin/change-password";
        }
    }
    
    @GetMapping("/backup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String backupPage(Model model) {
        return "admin-backup";
    }
    
    @GetMapping("/backup/download")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void downloadBackup(HttpServletResponse response) throws Exception {
        byte[] backupData = backupService.createBackup();
        
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=backup_" + 
                          java.time.LocalDateTime.now().toString().replace(":", "-") + ".zip");
        response.setContentLength(backupData.length);
        
        response.getOutputStream().write(backupData);
        response.getOutputStream().flush();
    }
    
    @PostMapping("/backup/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String restoreBackup(@RequestParam("backupFile") org.springframework.web.multipart.MultipartFile file,
                               RedirectAttributes ra) {
        try {
            if (file.isEmpty()) {
                ra.addFlashAttribute("error", "Please select a backup file to restore");
                return "redirect:/admin/backup";
            }
            
            byte[] backupData = file.getBytes();
            
            // Get backup info for confirmation
            java.util.Map<String, Object> backupInfo = backupService.getBackupInfo(backupData);
            
            // Store backup data in session for confirmation
            // In a real application, you'd want to handle this more securely
            ra.addFlashAttribute("backupInfo", backupInfo);
            ra.addFlashAttribute("backupData", backupData);
            
            return "redirect:/admin/backup/confirm";
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to process backup file: " + e.getMessage());
            return "redirect:/admin/backup";
        }
    }
    
    @GetMapping("/backup/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String confirmRestorePage(Model model) {
        // This would typically get backup info from session
        // For simplicity, we'll just show a generic confirmation
        return "admin-backup-confirm";
    }
    
    @PostMapping("/backup/restore/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String confirmRestore(RedirectAttributes ra) {
        try {
            // In a real implementation, you'd get the backup data from session
            // For now, we'll show a message that this requires manual confirmation
            ra.addFlashAttribute("info", "Restore functionality requires manual database restoration for safety");
            return "redirect:/admin/backup";
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Restore failed: " + e.getMessage());
            return "redirect:/admin/backup";
        }
    }
}
