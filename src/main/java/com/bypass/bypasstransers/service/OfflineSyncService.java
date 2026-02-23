package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.OfflineTransactionRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class OfflineSyncService {
    
    private final OfflineTransactionRepository offlineTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuditService auditService;
    
    public OfflineSyncService(OfflineTransactionRepository offlineTransactionRepository,
                             TransactionRepository transactionRepository,
                             WalletRepository walletRepository,
                             AuditService auditService) {
        this.offlineTransactionRepository = offlineTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.auditService = auditService;
    }
    
    /**
     * Sync all pending offline transactions to main transaction table
     */
    @Transactional
    public Map<String, Object> syncAllPendingTransactions() {
        List<OfflineTransaction> pendingTransactions = offlineTransactionRepository.findPendingTransactionsOrdered();
        Map<String, Object> result = new HashMap<>();
        
        int successCount = 0;
        int failedCount = 0;
        
        for (OfflineTransaction offlineTx : pendingTransactions) {
            try {
                syncSingleTransaction(offlineTx);
                successCount++;
            } catch (Exception e) {
                offlineTx.setSyncStatus("FAILED");
                offlineTx.setNotes(offlineTx.getNotes() + " | Sync failed: " + e.getMessage());
                offlineTransactionRepository.save(offlineTx);
                failedCount++;
                System.err.println("Failed to sync transaction ID " + offlineTx.getId() + ": " + e.getMessage());
            }
        }
        
        result.put("totalProcessed", pendingTransactions.size());
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }
    
    /**
     * Sync a single offline transaction
     */
    @Transactional
    public void syncSingleTransaction(OfflineTransaction offlineTx) {
        // Create main transaction from offline transaction
        Transaction mainTx = new Transaction();
        mainTx.setAmount(offlineTx.getAmount());
        mainTx.setFee(offlineTx.getFee());
        mainTx.setNetAmount(offlineTx.getNetAmount());
        mainTx.setType(offlineTx.getType());
        mainTx.setFromAccount(offlineTx.getFromAccount());
        mainTx.setToAccount(offlineTx.getToAccount());
        mainTx.setSyncStatus("SYNCED");
        mainTx.setCreatedBy(offlineTx.getUsername());
        mainTx.setDate(offlineTx.getOfflineRecordedAt());
        
        // Set wallet if fromAccount exists
        if (offlineTx.getFromAccount() != null) {
            List<Wallet> wallets = walletRepository.findByAccountType(offlineTx.getFromAccount());
            if (!wallets.isEmpty()) {
                mainTx.setWallet(wallets.get(0)); // Use the first wallet found
            }
        }
        
        // Save main transaction
        transactionRepository.save(mainTx);
        
        // Update offline transaction status
        offlineTx.setSyncStatus("SYNCED");
        offlineTx.setSyncedAt(LocalDateTime.now());
        offlineTx.setTransactionRef("SYNCED_" + mainTx.getId());
        offlineTransactionRepository.save(offlineTx);
        
        // Log audit
        auditService.logEntity("system", "offline_transactions", offlineTx.getId(), 
                              "SYNC_TRANSACTION", offlineTx.getUsername(), 
                              "Offline transaction synced to main system");
    }
    
    /**
     * Get sync statistics
     */
    public Map<String, Long> getSyncStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", offlineTransactionRepository.countPendingTransactions());
        stats.put("synced", offlineTransactionRepository.countSyncedTransactions());
        stats.put("failed", offlineTransactionRepository.countFailedTransactions());
        stats.put("total", offlineTransactionRepository.count());
        return stats;
    }
    
    /**
     * Retry failed transactions
     */
    @Transactional
    public int retryFailedTransactions() {
        List<OfflineTransaction> failedTransactions = offlineTransactionRepository.findBySyncStatus("FAILED");
        int retryCount = 0;
        
        for (OfflineTransaction failedTx : failedTransactions) {
            try {
                // Reset to pending and try again
                failedTx.setSyncStatus("PENDING");
                failedTx.setNotes(failedTx.getNotes() + " | Retrying sync at " + LocalDateTime.now());
                offlineTransactionRepository.save(failedTx);
                retryCount++;
            } catch (Exception e) {
                System.err.println("Failed to retry transaction ID " + failedTx.getId() + ": " + e.getMessage());
            }
        }
        
        return retryCount;
    }
    
    /**
     * Get user's offline transactions
     */
    public List<OfflineTransaction> getUserOfflineTransactions(String username) {
        return offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(username);
    }
    
    /**
     * Create offline transaction record
     */
    @Transactional
    public OfflineTransaction recordOfflineTransaction(OfflineTransaction offlineTx) {
        // Auto-generate transaction reference
        String ref = "OFFLINE_" + System.currentTimeMillis();
        offlineTx.setTransactionRef(ref);
        
        // Set default values
        if (offlineTx.getFee() == null) {
            offlineTx.setFee(0.0);
        }
        if (offlineTx.getNetAmount() == null) {
            offlineTx.setNetAmount(offlineTx.getAmount() - offlineTx.getFee());
        }
        
        return offlineTransactionRepository.save(offlineTx);
    }
}
