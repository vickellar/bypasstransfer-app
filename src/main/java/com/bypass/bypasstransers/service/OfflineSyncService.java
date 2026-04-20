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

import java.math.BigDecimal;
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
        return processSyncList(pendingTransactions);
    }
    
    /**
     * Sync pending offline transactions for a specific user
     */
    public Map<String, Object> syncUserPendingTransactions(String username) {
        List<OfflineTransaction> pendingTransactions = offlineTransactionRepository.findPendingTransactionsByUsernameOrdered(username);
        return processSyncList(pendingTransactions);
    }

    private Map<String, Object> processSyncList(List<OfflineTransaction> pendingTransactions) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (OfflineTransaction offlineTx : pendingTransactions) {
            try {
                syncSingleTransaction(offlineTx);
                successCount++;
            } catch (Exception e) {
                offlineTx.setSyncStatus("FAILED");
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                String newNotes = (offlineTx.getNotes() != null ? offlineTx.getNotes() + " | " : "") + "Sync failed: " + errorMessage;
                if (newNotes.length() > 1000) {
                    newNotes = newNotes.substring(0, 997) + "...";
                }
                offlineTx.setNotes(newNotes);
                offlineTransactionRepository.save(offlineTx);
                failedCount++;
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
        BigDecimal amount = offlineTx.getAmount() != null ? offlineTx.getAmount() : BigDecimal.ZERO;
        TransactionType type = offlineTx.getType();

        String providerAccountType = null;
        if (type == TransactionType.EXPENSE || type == TransactionType.OUTCOME) {
            providerAccountType = offlineTx.getFromAccount();
        } else if (type == TransactionType.TRANSFER) {
            providerAccountType = offlineTx.getToAccount();
        }

        BigDecimal fee = offlineTx.getFee() != null ? offlineTx.getFee() : BigDecimal.ZERO;
        if (type == TransactionType.INCOME) {
            fee = BigDecimal.ZERO;
        } else if (fee.compareTo(BigDecimal.ZERO) <= 0) {
            fee = ChargeCalculator.calculateTotalCharge(providerAccountType, amount);
        }

        BigDecimal netAmount = amount;

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

        Transaction mainTx = new Transaction();
        mainTx.setAmount(amount);
        mainTx.setFee(fee);
        mainTx.setNetAmount(netAmount);
        mainTx.setType(type);
        mainTx.setFromAccount(offlineTx.getFromAccount() != null ? offlineTx.getFromAccount().trim() : null);
        mainTx.setToAccount(offlineTx.getToAccount() != null ? offlineTx.getToAccount().trim() : null);
        mainTx.setSyncStatus("SYNCED");
        mainTx.setCreatedBy(offlineTx.getUsername());
        mainTx.setDate(offlineTx.getOfflineRecordedAt());
        mainTx.setCurrency(fromWallet != null ? fromWallet.getCurrency() : (toWallet != null ? toWallet.getCurrency() : Currency.USD));
        mainTx.setWallet(fromWallet != null ? fromWallet : toWallet);

        if ((type == TransactionType.EXPENSE || type == TransactionType.OUTCOME || type == TransactionType.TRANSFER) && fromWallet == null) {
             throw new IllegalStateException("Missing source wallet '" + offlineTx.getFromAccount() + "' for " + type);
        }
        if (type == TransactionType.INCOME && toWallet == null) {
             throw new IllegalStateException("Missing destination wallet '" + offlineTx.getToAccount() + "' for INCOME");
        }

        BigDecimal totalDeduction = BigDecimal.ZERO;
        if (type == TransactionType.EXPENSE || type == TransactionType.OUTCOME || type == TransactionType.TRANSFER) {
            totalDeduction = amount.add(fee);
            if (fromWallet != null) {
                if (offlineTx.getReferenceBalance() == null) {
                    offlineTx.setReferenceBalance(fromWallet.getBalance());
                }
                
                if (fromWallet.getBalance().compareTo(totalDeduction) < 0) {
                    throw new IllegalStateException("Insufficient wallet balance for sync. Required: " + totalDeduction + ", Available: " + fromWallet.getBalance());
                }
            }
        }

        Transaction savedMainTx;
        try {
            savedMainTx = transactionRepository.save(mainTx);
        } catch (Exception e) {
            if (type == TransactionType.OUTCOME) {
                mainTx.setType(TransactionType.EXPENSE);
                savedMainTx = transactionRepository.save(mainTx);
            } else {
                throw e;
            }
        }

        if (type == TransactionType.EXPENSE || type == TransactionType.OUTCOME) {
            if (fromWallet != null) {
                fromWallet.setBalance(fromWallet.getBalance().subtract(totalDeduction));
                walletRepository.save(fromWallet);
                auditService.logEntity("system", "wallets", fromWallet.getId(),
                        "BALANCE_ADJUSTMENT", "OfflineSync",
                        "Balance adjusted by -" + totalDeduction + " after syncing offline tx " + (savedMainTx != null ? savedMainTx.getId() : "NEW"));
            }
        } else if (type == TransactionType.TRANSFER) {
            if (fromWallet != null) {
                fromWallet.setBalance(fromWallet.getBalance().subtract(totalDeduction));
                walletRepository.save(fromWallet);
                auditService.logEntity("system", "wallets", fromWallet.getId(),
                        "BALANCE_ADJUSTMENT", "OfflineSync",
                        "Balance adjusted by -" + totalDeduction + " after syncing offline tx " + (savedMainTx != null ? savedMainTx.getId() : "NEW"));
            }
            if (toWallet != null) {
                toWallet.setBalance(toWallet.getBalance().add(amount));
                walletRepository.save(toWallet);
                auditService.logEntity("system", "wallets", toWallet.getId(),
                        "BALANCE_ADJUSTMENT", "OfflineSync",
                        "Balance adjusted by +" + amount + " after syncing offline tx " + (savedMainTx != null ? savedMainTx.getId() : "NEW"));
            }
        } else if (type == TransactionType.INCOME) {
            if (toWallet != null) {
                toWallet.setBalance(toWallet.getBalance().add(amount));
                walletRepository.save(toWallet);
                auditService.logEntity("system", "wallets", toWallet.getId(),
                        "BALANCE_ADJUSTMENT", "OfflineSync",
                        "Balance adjusted by +" + amount + " after syncing offline tx " + (savedMainTx != null ? savedMainTx.getId() : "NEW"));
            }
        }

        offlineTx.setFee(fee);
        offlineTx.setNetAmount(netAmount);
        offlineTx.setSyncStatus("SYNCED");
        offlineTx.setSyncedAt(LocalDateTime.now());
        offlineTx.setTransactionRef("SYNCED_" + (savedMainTx != null ? savedMainTx.getId() : "NEW"));
        offlineTransactionRepository.save(offlineTx);

        auditService.logEntity("system", "offline_transactions", offlineTx.getId(),
                "SYNC_TRANSACTION", offlineTx.getUsername(),
                "Offline transaction synced with financial adjustment applied");
    }
    
    public Map<String, Long> getUserSyncStatistics(String username) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", (long) offlineTransactionRepository.findByUsernameAndSyncStatus(username, "PENDING").size());
        stats.put("synced", (long) offlineTransactionRepository.findByUsernameAndSyncStatus(username, "SYNCED").size());
        stats.put("failed", (long) offlineTransactionRepository.findByUsernameAndSyncStatus(username, "FAILED").size());
        stats.put("total", (long) offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(username).size());
        return stats;
    }
        
    public Map<String, Long> getSyncStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", offlineTransactionRepository.countPendingTransactions());
        stats.put("synced", offlineTransactionRepository.countSyncedTransactions());
        stats.put("failed", offlineTransactionRepository.countFailedTransactions());
        stats.put("total", offlineTransactionRepository.count());
        return stats;
    }
    
    public int retryFailedTransactions() {
        List<OfflineTransaction> failedTransactions = offlineTransactionRepository.findBySyncStatus("FAILED");
        int retryCount = 0;
        for (OfflineTransaction failedTx : failedTransactions) {
            try {
                failedTx.setSyncStatus("PENDING");
                failedTx.setNotes((failedTx.getNotes() != null ? failedTx.getNotes() + " | " : "") + "Retrying sync at " + LocalDateTime.now());
                offlineTransactionRepository.save(failedTx);
                retryCount++;
            } catch (Exception e) {}
        }
        return retryCount;
    }
    
    public List<OfflineTransaction> getUserOfflineTransactions(String username) {
        return offlineTransactionRepository.findByUsernameOrderByOfflineRecordedAtDesc(username);
    }
    
    public OfflineTransaction recordOfflineTransaction(OfflineTransaction offlineTx) {
        String ref = "OFFLINE_" + System.currentTimeMillis();
        offlineTx.setTransactionRef(ref);
        
        if (offlineTx.getFee() == null) {
            offlineTx.setFee(BigDecimal.ZERO);
        }
        if (offlineTx.getNetAmount() == null) {
            offlineTx.setNetAmount(offlineTx.getAmount());
        }

        if (offlineTx.getReferenceBalance() == null && offlineTx.getFromAccount() != null) {
            walletRepository.findByOwnerIdAndAccountType(offlineTx.getUserId(), offlineTx.getFromAccount())
                .stream().findFirst().ifPresent(w -> offlineTx.setReferenceBalance(w.getBalance()));
        }
        
        return offlineTransactionRepository.save(offlineTx);
    }
}
