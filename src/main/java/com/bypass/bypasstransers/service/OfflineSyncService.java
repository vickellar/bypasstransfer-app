package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.enums.Currency;
import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.OfflineTransactionRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.util.ChargeCalculator;
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
        // Decide provider account type and calculate fee if missing.
        double amount = offlineTx.getAmount() != null ? offlineTx.getAmount() : 0.0;
        TransactionType type = offlineTx.getType();

        String providerAccountType = null;
        if (type == TransactionType.EXPENSE || type == TransactionType.OUTCOME) {
            providerAccountType = offlineTx.getFromAccount();
        } else if (type == TransactionType.TRANSFER) {
            providerAccountType = offlineTx.getToAccount(); // receiving method chosen
        }

        double fee = offlineTx.getFee() != null ? offlineTx.getFee() : 0.0;
        if (type == TransactionType.INCOME) {
            fee = 0.0;
        } else if (fee <= 0.0) {
            fee = ChargeCalculator.calculateTotalCharge(providerAccountType, amount);
        }

        // In this system, credited/transferred amount is the net amount.
        double netAmount = amount;

        // Find source and destination wallets (owner-scoped).
        Wallet fromWallet = null;
        Wallet toWallet = null;
        if (offlineTx.getFromAccount() != null) {
            fromWallet = walletRepository
                    .findByOwnerIdAndAccountType(offlineTx.getUserId(), offlineTx.getFromAccount())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        if (offlineTx.getToAccount() != null) {
            toWallet = walletRepository
                    .findByOwnerIdAndAccountType(offlineTx.getUserId(), offlineTx.getToAccount())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        // Create main transaction from offline transaction
        Transaction mainTx = new Transaction();
        mainTx.setAmount(amount);
        mainTx.setFee(fee);
        mainTx.setNetAmount(netAmount);
        mainTx.setType(type);
        mainTx.setFromAccount(offlineTx.getFromAccount());
        mainTx.setToAccount(offlineTx.getToAccount());
        mainTx.setSyncStatus("SYNCED");
        mainTx.setCreatedBy(offlineTx.getUsername());
        mainTx.setDate(offlineTx.getOfflineRecordedAt());
        mainTx.setCurrency(fromWallet != null ? fromWallet.getCurrency() : (toWallet != null ? toWallet.getCurrency() : Currency.USD));
        // For audit purposes, attach the source wallet when possible
        mainTx.setWallet(fromWallet != null ? fromWallet : toWallet);

        // Save main transaction first
        Transaction savedMainTx = transactionRepository.save(mainTx);

        // Update wallet balances based on transaction type
        // - EXPENSE/OUTCOME: deduct amount + fee from fromWallet
        // - TRANSFER: deduct amount + fee from fromWallet, credit amount to toWallet
        // - INCOME: credit amount to toWallet
        try {
            if (type == TransactionType.EXPENSE || type == TransactionType.OUTCOME) {
                if (fromWallet != null) {
                    double totalDeduction = amount + fee;
                    fromWallet.setBalance(fromWallet.getBalance() - totalDeduction);
                    walletRepository.save(fromWallet);
                    auditService.logEntity("system", "wallets", fromWallet.getId(),
                            "BALANCE_ADJUSTMENT", "OfflineSync",
                            "Balance adjusted by -" + totalDeduction + " after syncing offline transaction " + savedMainTx.getId());
                }
            } else if (type == TransactionType.TRANSFER) {
                if (fromWallet != null) {
                    double totalDeduction = amount + fee;
                    fromWallet.setBalance(fromWallet.getBalance() - totalDeduction);
                    walletRepository.save(fromWallet);
                    auditService.logEntity("system", "wallets", fromWallet.getId(),
                            "BALANCE_ADJUSTMENT", "OfflineSync",
                            "Balance adjusted by -" + totalDeduction + " after syncing offline transaction " + savedMainTx.getId());
                }
                if (toWallet != null) {
                    toWallet.setBalance(toWallet.getBalance() + amount);
                    walletRepository.save(toWallet);
                    auditService.logEntity("system", "wallets", toWallet.getId(),
                            "BALANCE_ADJUSTMENT", "OfflineSync",
                            "Balance adjusted by +" + amount + " after syncing offline transaction " + savedMainTx.getId());
                }
            } else if (type == TransactionType.INCOME) {
                if (toWallet != null) {
                    toWallet.setBalance(toWallet.getBalance() + amount);
                    walletRepository.save(toWallet);
                    auditService.logEntity("system", "wallets", toWallet.getId(),
                            "BALANCE_ADJUSTMENT", "OfflineSync",
                            "Balance adjusted by +" + amount + " after syncing offline transaction " + savedMainTx.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to update wallet balances for offline transaction ID " + offlineTx.getId() + ": " + e.getMessage());
            throw e;
        }

        // Persist calculated values back to the offline record as well
        offlineTx.setFee(fee);
        offlineTx.setNetAmount(netAmount);

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
            // Fees are charged on top; netAmount is the credited/transferred amount.
            offlineTx.setNetAmount(offlineTx.getAmount());
        }
        
        return offlineTransactionRepository.save(offlineTx);
    }
}
