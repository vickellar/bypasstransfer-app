package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.dto.TransactionSummary;
import com.bypass.bypasstransers.dto.UserTransactionSummary;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.service.AlertService;
import com.bypass.bypasstransers.service.ReconsiliationService;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.service.TransactionService;
import com.bypass.bypasstransers.service.WalletService;
import com.bypass.bypasstransers.service.WalletTransactionService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;


@Controller
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private TransactionService service;

    @Autowired
    private ReconsiliationService reconService;

    @Autowired
    private DailyReconciliationRepository dailyRepo;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletTransactionService walletTransactionService;

    /**
     * Main dashboard - routes to appropriate view based on user role
     */
    @GetMapping("/app")
    public String dashboard(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Route to appropriate dashboard based on role
        if (securityService.isSupervisorOrAbove()) {
            return supervisorDashboard(model);
        } else {
            return staffDashboard(model);
        }
    }

    /**
     * Staff dashboard - shows only their own data
     */
    @GetMapping("/app/staff")
    public String staffDashboard(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get staff-specific data
        List<Wallet> wallets = walletService.getCurrentUserWallets();
        Double totalBalance = walletService.getCurrentUserTotalBalance();
        long walletCount = walletService.countCurrentUserWallets();
        TransactionSummary txSummary = service.getCurrentUserSummary();

        model.addAttribute("user", currentUser);
        model.addAttribute("wallets", wallets);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("walletCount", walletCount);
        model.addAttribute("txSummary", txSummary);

        return "staff-dashboard";
    }

    /**
     * Supervisor/Admin dashboard - shows company overview
     */
    @GetMapping("/app/supervisor")
    public String supervisorDashboard(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Ensure only supervisors and above
        if (!securityService.isSupervisorOrAbove()) {
            return "redirect:/app";
        }

        // Get company-wide data
        List<Wallet> allWallets = walletService.getAllWallets();
        Double companyBalance = walletService.getCompanyTotalBalance();
        List<UserTransactionSummary> userSummaries = service.getCompanyTransactionSummary();

        model.addAttribute("user", currentUser);
        model.addAttribute("allWallets", allWallets);
        model.addAttribute("companyBalance", companyBalance);
        model.addAttribute("userSummaries", userSummaries);
        model.addAttribute("isSupervisor", securityService.isSupervisorOrAbove());
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());

        return "supervisor-dashboard";
    }

    // Explicit dashboard mapping used as post-login landing to avoid redirect loops
    @GetMapping("/dashboard")
    public String dashboardAlias(Model model) {
        return dashboard(model);
    }

    @PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/receive")
    public String receive(@RequestParam String account, @RequestParam double amount, RedirectAttributes ra) {
        try {
            walletTransactionService.receive(account, amount);
            ra.addFlashAttribute("success", "Received $" + amount + " into " + account);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to receive: " + ex.getMessage());
        }
        return "redirect:/app";
    }

    @PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/send")
    public String send(@RequestParam String account, @RequestParam double amount, RedirectAttributes ra) {
        try {
            walletTransactionService.send(account, amount);
            ra.addFlashAttribute("success", "Sent $" + amount + " from " + account);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to send: " + ex.getMessage());
        }
        return "redirect:/app";
    }

    @PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/transfer")
    public String transfer(@RequestParam String from, @RequestParam String to, @RequestParam double amount, RedirectAttributes ra) {
        try {
            walletTransactionService.transfer(from, to, amount);
            ra.addFlashAttribute("success", "Transferred $" + amount + " from " + from + " to " + to);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to transfer: " + ex.getMessage());
        }
        return "redirect:/app";
    }

    // Handle reconciliation form POST from the dashboard
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
    @PostMapping("/reconcile")
    public String reconcile(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam double actualBalance,
            RedirectAttributes redirectAttributes) {
        LocalDate d = date != null ? date : LocalDate.now();
        try {
            reconService.reconcile(d, actualBalance);
            redirectAttributes.addFlashAttribute("success", "Reconciliation saved for " + d);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Reconciliation failed: " + ex.getMessage());
        }
        return "redirect:/app";
    }
}