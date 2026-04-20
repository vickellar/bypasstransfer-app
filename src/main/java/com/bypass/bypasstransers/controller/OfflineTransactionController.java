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
@PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
    public String syncAllTransactions(RedirectAttributes ra) {
        try {
            Map<String, Object> result = offlineSyncService.syncAllPendingTransactions();

            String message = String.format("Sync completed: %d processed, %d successful, %d failed",
                    result.get("totalProcessed"),
                    result.get("successCount"),
                    result.get("failedCount"));

            if ((Integer) result.get("failedCount") > 0) {
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
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
    public String syncManagement(Model model) {
        User currentUser = securityService.getCurrentUser();
        List<OfflineTransaction> transactions = offlineSyncService
                .getUserOfflineTransactions(currentUser.getUsername());

        model.addAttribute("transactions", transactions);
        model.addAttribute("user", currentUser);
        model.addAttribute("syncStats", offlineSyncService.getSyncStatistics());

        return "sync-management";
    }

    @PostMapping("/sync/retry")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> apiSyncAllTransactions() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = offlineSyncService.syncAllPendingTransactions();

            String message = String.format("Sync completed: %d processed, %d successful, %d failed",
                    (Integer) result.get("totalProcessed"),
                    (Integer) result.get("successCount"),
                    (Integer) result.get("failedCount"));

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
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
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
