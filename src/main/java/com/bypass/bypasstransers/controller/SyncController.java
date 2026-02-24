package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.OfflineTransactionRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.service.OfflineSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
public class SyncController {

    @Autowired
    private OfflineSyncService offlineSyncService;

    @Autowired
    private OfflineTransactionRepository offlineTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @GetMapping("/sync")
    public String syncDashboard(Model model) {
        // Get sync statistics
        Map<String, Long> stats = offlineSyncService.getSyncStatistics();
        model.addAttribute("syncStats", stats);

        // Get recent offline transactions
        List<OfflineTransaction> recentOffline = offlineTransactionRepository.findTop10ByOrderByOfflineRecordedAtDesc();
        model.addAttribute("recentOffline", recentOffline);

        // Get recent synced transactions
        model.addAttribute("recentSynced", transactionRepository.findAll());

        return "sync-dashboard";
    }

    @PostMapping("/sync/process-all")
    public String processAllPending(RedirectAttributes ra) {
        try {
            Map<String, Object> result = offlineSyncService.syncAllPendingTransactions();
            ra.addFlashAttribute("success", 
                "Sync completed: " + result.get("successCount") + " succeeded, " + 
                result.get("failedCount") + " failed out of " + result.get("totalProcessed") + " total");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Sync failed: " + e.getMessage());
        }
        return "redirect:/sync";
    }

    @PostMapping("/sync/retry-failed")
    public String retryFailedTransactions(RedirectAttributes ra) {
        try {
            int retryCount = offlineSyncService.retryFailedTransactions();
            ra.addFlashAttribute("success", "Retried " + retryCount + " failed transactions");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Retry failed: " + e.getMessage());
        }
        return "redirect:/sync";
    }

    @GetMapping("/users/{userId}/transactions")
    public String getUserTransactions(@PathVariable Long userId, Model model) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Get user's wallets
        List<Wallet> userWallets = walletRepository.findByOwnerId(userId);
        model.addAttribute("user", user);
        model.addAttribute("wallets", userWallets);

        // Get all transactions related to user's wallets
        List<com.bypass.bypasstransers.model.Transaction> userTransactions = 
            transactionRepository.findByWalletOwnerId(userId);
        model.addAttribute("transactions", userTransactions);

        // Get offline transactions for this user
        List<OfflineTransaction> offlineTransactions = 
            offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(user.getUsername());
        model.addAttribute("offlineTransactions", offlineTransactions);

        return "user-transactions";
    }
    
    @PostMapping("/sync/process-single")
    public String processSingleTransaction(@RequestParam Long id, RedirectAttributes ra) {
        try {
            OfflineTransaction offlineTx = offlineTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offline transaction not found"));
            offlineSyncService.syncSingleTransaction(offlineTx);
            ra.addFlashAttribute("success", "Transaction synced successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Sync failed: " + e.getMessage());
        }
        return "redirect:/sync";
    }
}