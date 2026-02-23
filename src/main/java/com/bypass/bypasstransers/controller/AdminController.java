package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.service.UserProvisioningService;
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

    public AdminController(UserRepository userRepository, WalletRepository walletRepository, 
                          TransactionRepository transactionRepository, SecurityService securityService,
                          UserProvisioningService userProvisioningService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.securityService = securityService;
        this.userProvisioningService = userProvisioningService;
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
        
        return "reports";
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
}
