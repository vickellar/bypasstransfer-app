package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.enums.Currency;
import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.OfflineTransactionRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
                // Handle the exception without propagating it to avoid marking the entire transaction for rollback
                offlineTx.setSyncStatus("FAILED");
                offlineTx.setNotes((offlineTx.getNotes() != null ? offlineTx.getNotes() + " | " : "") + "Sync failed: " + e.getMessage());
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        Wallet wallet = null;
        if (offlineTx.getFromAccount() != null) {
            List<Wallet> wallets = walletRepository.findByAccountType(offlineTx.getFromAccount());
            if (!wallets.isEmpty()) {
                wallet = wallets.get(0); // Use the first wallet found
                mainTx.setWallet(wallet);
            }
        }
        
        // Save main transaction first
        Transaction savedMainTx = transactionRepository.save(mainTx);
        
        // Update user's account balance based on transaction type
        if (wallet != null) {
            try {
                // Adjust wallet balance based on transaction type
                if (offlineTx.getType() == TransactionType.EXPENSE || offlineTx.getType() == TransactionType.TRANSFER) {
                    // For expenses and transfers OUT, deduct from wallet (amount + fee)
                    double totalDeduction = offlineTx.getAmount() + offlineTx.getFee();
                    wallet.setBalance(wallet.getBalance() - totalDeduction);
                } else if (offlineTx.getType() == TransactionType.INCOME) {
                    // For income, add to wallet (only the amount, no fees for income)
                    wallet.setBalance(wallet.getBalance() + offlineTx.getAmount());
                }
                walletRepository.save(wallet);
                
                // Log the financial adjustment
                auditService.logEntity("system", "wallets", wallet.getId(), 
                        "BALANCE_ADJUSTMENT", 
                        "OfflineSync", 
                        "Balance adjusted by " + (offlineTx.getType() == TransactionType.INCOME ? "+" : "-") + 
                        (offlineTx.getType() == TransactionType.INCOME ? offlineTx.getAmount() : 
                         (offlineTx.getAmount() + offlineTx.getFee())) + 
                        " after syncing offline transaction " + savedMainTx.getId());
            } catch (Exception e) {
                // If wallet update fails, we should still complete the sync but log the error
                System.err.println("Failed to update wallet balance for transaction ID " + offlineTx.getId() + ": " + e.getMessage());
                throw e; // Re-throw to ensure transaction rolls back if wallet update fails
            }
        }
        
        // Update offline transaction status
        offlineTx.setSyncStatus("SYNCED");
        offlineTx.setSyncedAt(LocalDateTime.now());
        offlineTx.setTransactionRef("SYNCED_" + savedMainTx.getId());
        offlineTransactionRepository.save(offlineTx);
        
        // Log audit
        auditService.logEntity("system", "offline_transactions", offlineTx.getId(), 
                              "SYNC_TRANSACTION", offlineTx.getUsername(), 
                              "Offline transaction synced to main system with financial adjustment applied");
    }
    
    /**
     * Get sync statistics for a specific user
     */
    public Map<String, Long> getUserSyncStatistics(String username) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", (long) offlineTransactionRepository.findByUsernameAndSyncStatus(username, "PENDING").size());
        stats.put("synced", (long) offlineTransactionRepository.findByUsernameAndSyncStatus(username, "SYNCED").size());
        stats.put("failed", (long) offlineTransactionRepository.findByUsernameAndSyncStatus(username, "FAILED").size());
        stats.put("total", (long) offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(username).size());
        return stats;
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
    public int retryFailedTransactions() {
        List<OfflineTransaction> failedTransactions = offlineTransactionRepository.findBySyncStatus("FAILED");
        int retryCount = 0;
        
        for (OfflineTransaction failedTx : failedTransactions) {
            try {
                // Reset to pending and try again
                failedTx.setSyncStatus("PENDING");
                failedTx.setNotes((failedTx.getNotes() != null ? failedTx.getNotes() + " | " : "") + "Retrying sync at " + LocalDateTime.now());
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
