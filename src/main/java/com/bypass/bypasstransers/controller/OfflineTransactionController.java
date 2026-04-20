package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.service.OfflineSyncService;
import com.bypass.bypasstransers.service.SecurityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/offline")
@PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN','SUPERVISOR')")
public class OfflineTransactionController {

    private final OfflineSyncService offlineSyncService;
    private final SecurityService securityService;
    private final AccountRepository accountRepository;

    public OfflineTransactionController(OfflineSyncService offlineSyncService,
            SecurityService securityService,
            AccountRepository accountRepository) {
        this.offlineSyncService = offlineSyncService;
        this.securityService = securityService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/transactions")
    public String listOfflineTransactions(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) return "redirect:/login";
        
        List<OfflineTransaction> transactions = offlineSyncService
                .getUserOfflineTransactions(currentUser.getUsername());

        model.addAttribute("transactions", transactions);
        model.addAttribute("user", currentUser);
        model.addAttribute("syncStats", offlineSyncService.getUserSyncStatistics(currentUser.getUsername()));

        return "offline-transactions";
    }

    @GetMapping("/transactions/new")
    public String newOfflineTransactionForm(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("transaction", new OfflineTransaction());
        model.addAttribute("user", currentUser);
        model.addAttribute("accounts", accountRepository.findByOwnerId(currentUser.getId()));
        model.addAttribute("transactionTypes", com.bypass.bypasstransers.enums.TransactionType.values());
        return "offline-transaction-form";
    }

    @PostMapping("/transactions/save")
    public String saveOfflineTransaction(@ModelAttribute OfflineTransaction transaction,
            RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            if (currentUser == null) {
                ra.addFlashAttribute("error", "Session expired. Please log in again.");
                return "redirect:/login";
            }

            // Set user information
            transaction.setUserId(currentUser.getId());
            transaction.setUsername(currentUser.getUsername());
            transaction.setOfflineRecordedAt(LocalDateTime.now());

            // Validate required fields
            if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                ra.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/offline/transactions/new";
            }

            if (transaction.getType() == null) {
                ra.addFlashAttribute("error", "Transaction type is required");
                return "redirect:/offline/transactions/new";
            }

            // Save offline transaction
            OfflineTransaction saved = offlineSyncService.recordOfflineTransaction(transaction);

            ra.addFlashAttribute("success",
                    "Offline transaction recorded successfully. Transaction ID: " + saved.getTransactionRef());
            return "redirect:/offline/transactions";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to record transaction: " + e.getMessage());
            return "redirect:/offline/transactions/new";
        }
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN','SUPERVISOR')")
    public String syncAllTransactions(RedirectAttributes ra) {
        try {
            User currentUser = securityService.getCurrentUser();
            if (currentUser == null) {
                ra.addFlashAttribute("error", "Session expired. Please log in again.");
                return "redirect:/login";
            }
            
            Map<String, Object> result = offlineSyncService.syncUserPendingTransactions(currentUser.getUsername());

            int total = (result.get("totalProcessed") instanceof Number) ? ((Number) result.get("totalProcessed")).intValue() : 0;
            int success = (result.get("successCount") instanceof Number) ? ((Number) result.get("successCount")).intValue() : 0;
            int failed = (result.get("failedCount") instanceof Number) ? ((Number) result.get("failedCount")).intValue() : 0;

            String message = String.format("Sync completed: %d processed, %d successful, %d failed",
                    total, success, failed);

            if (failed > 0) {
                ra.addFlashAttribute("warning", message);
            } else {
                ra.addFlashAttribute("success", message);
            }

            return "redirect:/offline/transactions";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Sync failed: " + e.getMessage());
            return "redirect:/offline/transactions";
        }
    }

    @GetMapping("/sync-management")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN','SUPERVISOR')")
    public String syncManagement(Model model) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) return "redirect:/login";
        
        List<OfflineTransaction> transactions = offlineSyncService
                .getUserOfflineTransactions(currentUser.getUsername());

        model.addAttribute("transactions", transactions);
        model.addAttribute("user", currentUser);
        model.addAttribute("syncStats", offlineSyncService.getSyncStatistics());

        return "sync-management";
    }

    @PostMapping("/sync/retry")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN','SUPERVISOR')")
    public String retryFailedTransactions(RedirectAttributes ra) {
        try {
            int retryCount = offlineSyncService.retryFailedTransactions();
            ra.addFlashAttribute("success", "Retrying " + retryCount + " failed transactions");
            return "redirect:/offline/transactions";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Retry failed: " + e.getMessage());
            return "redirect:/offline/transactions";
        }
    }

    @PostMapping("/api/sync")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> apiSyncAllTransactions() {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = securityService.getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Session expired");
                return ResponseEntity.status(401).body(response);
            }
            Map<String, Object> result = offlineSyncService.syncUserPendingTransactions(currentUser.getUsername());

            int total = (result.get("totalProcessed") instanceof Number) ? ((Number) result.get("totalProcessed")).intValue() : 0;
            int success = (result.get("successCount") instanceof Number) ? ((Number) result.get("successCount")).intValue() : 0;
            int failed = (result.get("failedCount") instanceof Number) ? ((Number) result.get("failedCount")).intValue() : 0;

            String message = String.format("Sync completed: %d processed, %d successful, %d failed",
                    total, success, failed);

            response.put("success", true);
            response.put("message", message);
            response.put("result", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Sync failed: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/sync/retry")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> apiRetryFailedTransactions() {
        Map<String, Object> response = new HashMap<>();
        try {
            int retryCount = offlineSyncService.retryFailedTransactions();
            response.put("success", true);
            response.put("message", "Retried " + retryCount + " failed transactions");
            response.put("retryCount", retryCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Retry failed: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
