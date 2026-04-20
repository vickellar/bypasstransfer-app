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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for general administrative tasks and overview.
 */
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
    private final com.bypass.bypasstransers.service.ExchangeRateService exchangeRateService;

    public AdminController(UserRepository userRepository, 
                          WalletRepository walletRepository, 
                          TransactionRepository transactionRepository, 
                          SecurityService securityService,
                          UserProvisioningService userProvisioningService,
                          PasswordEncoder passwordEncoder,
                          BackupService backupService,
                          com.bypass.bypasstransers.service.ExchangeRateService exchangeRateService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.securityService = securityService;
        this.userProvisioningService = userProvisioningService;
        this.passwordEncoder = passwordEncoder;
        this.backupService = backupService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping({"/", ""})
    public String adminHome(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        long totalUsers = userRepository.count();
        long staffCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.STAFF).count();
        long supervisorCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.SUPERVISOR).count();
        long adminCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN).count();
        
        List<Wallet> allWallets = walletRepository.findAll();
        
        java.util.Map<com.bypass.bypasstransers.enums.Currency, BigDecimal> balanceByCurrency = allWallets.stream()
            .filter(w -> w.getCurrency() != null)
            .collect(Collectors.groupingBy(
                Wallet::getCurrency,
                Collectors.mapping(Wallet::getBalance, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
            
        BigDecimal totalBalanceInUsd = allWallets.stream()
            .filter(w -> w.getCurrency() != null)
            .map(w -> {
                try {
                    return exchangeRateService.convert(w.getBalance(), w.getCurrency().name(), "USD");
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("staffCount", staffCount);
        model.addAttribute("supervisorCount", supervisorCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("totalWallets", allWallets.size());
        model.addAttribute("totalBalanceInUsd", totalBalanceInUsd != null ? totalBalanceInUsd.doubleValue() : 0.0);
        model.addAttribute("balanceByCurrency", balanceByCurrency);
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        
        return "admin";
    }
    
    @GetMapping("/profit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public String viewProfit(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null || !securityService.isSuperAdmin()) {
            return "redirect:/admin";
        }
        
        List<Transaction> allTransactions = transactionRepository.findAll();
        
        java.util.Map<com.bypass.bypasstransers.enums.Currency, BigDecimal> profitByCurrency = allTransactions.stream()
            .filter(t -> t.getType() != null && t.getType() != TransactionType.INCOME && t.getCurrency() != null)
            .collect(Collectors.groupingBy(
                Transaction::getCurrency,
                Collectors.mapping(t -> t.getAmount().multiply(ChargeCalculator.BASE_PROFIT_DEFAULT),
                                 Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
            
        BigDecimal totalProfitInUsd = profitByCurrency.entrySet().stream()
            .map(entry -> {
                try {
                    return exchangeRateService.convert(entry.getValue(), entry.getKey().name(), "USD");
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalTransactionsCount = allTransactions.size();
            
        model.addAttribute("profitByCurrency", profitByCurrency);
        model.addAttribute("totalProfitInUsd", totalProfitInUsd != null ? totalProfitInUsd.doubleValue() : 0.0);
        model.addAttribute("totalTransactions", totalTransactionsCount);
        model.addAttribute("user", currentUser);
        
        return "admin-profit";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','SUPER_ADMIN')")
    public String reportsPage(Model model) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        
        java.util.Map<com.bypass.bypasstransers.enums.Currency, BigDecimal> amountByCurrency = allTransactions.stream()
            .filter(t -> t.getCurrency() != null)
            .collect(Collectors.groupingBy(
                Transaction::getCurrency,
                Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
            
        java.util.Map<com.bypass.bypasstransers.enums.Currency, BigDecimal> feesByCurrency = allTransactions.stream()
            .filter(t -> t.getCurrency() != null)
            .collect(Collectors.groupingBy(
                Transaction::getCurrency,
                Collectors.mapping(Transaction::getFee, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
            
        java.util.Map<com.bypass.bypasstransers.enums.Currency, BigDecimal> netByCurrency = allTransactions.stream()
            .filter(t -> t.getCurrency() != null)
            .collect(Collectors.groupingBy(
                Transaction::getCurrency,
                Collectors.mapping(Transaction::getNetAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
            
        model.addAttribute("totalTransactions", allTransactions.size());
        model.addAttribute("amountByCurrency", amountByCurrency);
        model.addAttribute("feesByCurrency", feesByCurrency);
        model.addAttribute("netByCurrency", netByCurrency);
        
        List<User> staffMembers = userRepository.findAll().stream()
                .filter(u -> u.getIsActive() && (u.getRole() == Role.STAFF || u.getRole() == Role.SUPERVISOR))
                .collect(Collectors.toList());
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

    @PostMapping("/migrate-users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String migrateOldUsers(RedirectAttributes ra) {
        List<User> allUsers = userRepository.findAll();
        int migratedCount = 0;
        int skippedCount = 0;
        
        for (User user : allUsers) {
            try {
                if (!userProvisioningService.hasDefaultWallets(user)) {
                    userProvisioningService.createDefaultWalletsForUser(user);
                    migratedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                System.err.println("Failed to migrate user " + user.getUsername() + ": " + e.getMessage());
            }
        }
        
        ra.addFlashAttribute("success", "Migration complete: " + migratedCount + " users migrated, " + skippedCount + " already have wallets.");
        return "redirect:/admin";
    }

    @PostMapping("/create-wallets")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String createWalletsForUser(@RequestParam(required = true) Long userId, RedirectAttributes ra) {
        Objects.requireNonNull(userId, "User ID must not be null");
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
                ra.addFlashAttribute("success", "Default wallets created for " + user.getUsername());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create wallets: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
    
    @PostMapping("/activate-user")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String activateUser(@RequestParam(required = true) Long userId, RedirectAttributes ra) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/users";
        }
        
        User user = userOpt.get();
        try {
            user.setIsActive(true);
            user.setDeletedAt(null);
            userRepository.save(user);
            userProvisioningService.createDefaultWalletsForUser(user);
            ra.addFlashAttribute("success", "User " + user.getUsername() + " activated and wallets created.");
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
            Objects.requireNonNull(currentUser, "Current user must not be null");
            Optional<User> userOpt = userRepository.findById(currentUser.getId());
            
            if (userOpt.isEmpty()) {
                ra.addFlashAttribute("error", "User not found");
                return "redirect:/admin/change-password";
            }
            
            User user = userOpt.get();
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                ra.addFlashAttribute("error", "Current password incorrect");
                return "redirect:/admin/change-password";
            }
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/admin/change-password";
            }
            if (newPassword.length() < 8) {
                ra.addFlashAttribute("error", "Password too short");
                return "redirect:/admin/change-password";
            }
            
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            ra.addFlashAttribute("success", "Password changed");
            return "redirect:/admin";
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed: " + e.getMessage());
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
                ra.addFlashAttribute("error", "No file selected");
                return "redirect:/admin/backup";
            }
            byte[] backupData = file.getBytes();
            java.util.Map<String, Object> backupInfo = backupService.getBackupInfo(backupData);
            ra.addFlashAttribute("backupInfo", backupInfo);
            ra.addFlashAttribute("backupData", backupData);
            return "redirect:/admin/backup/confirm";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed: " + e.getMessage());
            return "redirect:/admin/backup";
        }
    }
    
    @GetMapping("/backup/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String confirmRestorePage(Model model) {
        return "admin-backup-confirm";
    }
    
    @PostMapping("/backup/restore/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String confirmRestore(RedirectAttributes ra) {
        ra.addFlashAttribute("info", "Restore requires manual database intervention for safety");
        return "redirect:/admin/backup";
    }
}
