package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.TransactionStatus;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletTransactionService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AuditService auditService;

    /**
     * Receive money (Cash In) - adds to wallet balance
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(String accountType, BigDecimal amount) {
        validateAmount(amount);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated");
        }

        // Find wallet by account type and current user
        List<Wallet> wallets = walletRepository.findByOwnerId(currentUser.getId());
        
        Wallet wallet = wallets.stream()
                .filter(w -> accountType != null && accountType.equalsIgnoreCase(w.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: '" + accountType + "'"));

        // Verify wallet exists in database by fetching it fresh
        Wallet dbWallet = walletRepository.findById(wallet.getId()).orElse(null);
        if (dbWallet == null) {
            throw new IllegalStateException("Wallet not found in database. Please contact admin.");
        }

        BigDecimal prevBalance = dbWallet.getBalance();
        dbWallet.setBalance(prevBalance.add(amount));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.INCOME);
        tx.setAmount(amount);
        tx.setFee(BigDecimal.ZERO);
        tx.setNetAmount(amount);
        tx.setToAccount(accountType);
        tx.setDate(LocalDateTime.now());
        tx.setWallet(dbWallet);
        tx.setCurrency(dbWallet.getCurrency());
        tx.setSyncStatus("SYNCED");
        tx.setStatus(TransactionStatus.APPROVED);

        walletRepository.save(dbWallet);
        transactionRepository.save(tx);
        
        auditService.logEntity(currentUser.getUsername(), "transactions", tx.getId(), "RECEIVE",
                prevBalance.toString(), dbWallet.getBalance().toString());
    }

    /**
     * Send money (Cash Out) - deducts from wallet balance
     */
    @Transactional(rollbackFor = Exception.class)
    public void send(String accountType, BigDecimal amount) {
        validateAmount(amount);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated");
        }

        // Find wallet by account type and current user
        List<Wallet> wallets = walletRepository.findByOwnerId(currentUser.getId());
        Wallet wallet = wallets.stream()
                .filter(w -> accountType.equalsIgnoreCase(w.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + accountType));

        // Total charge = 5% company profit + provider fee
        BigDecimal fee = ChargeCalculator.calculateTotalCharge(accountType, amount);
        BigDecimal total = amount.add(fee);

        if (wallet.getBalance().compareTo(total) < 0) {
            throw new IllegalStateException("Insufficient balance. Required: " + total + ", available: " + wallet.getBalance());
        }

        BigDecimal prevBalance = wallet.getBalance();
        wallet.setBalance(prevBalance.subtract(total));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setNetAmount(amount);
        tx.setFromAccount(accountType);
        tx.setDate(LocalDateTime.now());
        tx.setWallet(wallet);
        tx.setCurrency(wallet.getCurrency());
        tx.setSyncStatus("SYNCED");
        tx.setStatus(TransactionStatus.APPROVED);

        walletRepository.save(wallet);
        transactionRepository.save(tx);

        auditService.logEntity(currentUser.getUsername(), "transactions", tx.getId(), "SEND",
                prevBalance.toString(), wallet.getBalance().toString());
    }

    /**
     * Transfer between wallets
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String fromAccountType, String toAccountType, BigDecimal amount) {
        validateAmount(amount);
        
        if (fromAccountType.equalsIgnoreCase(toAccountType)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated");
        }

        // Find both wallets
        List<Wallet> wallets = walletRepository.findByOwnerId(currentUser.getId());
        
        Wallet fromWallet = wallets.stream()
                .filter(w -> fromAccountType.equalsIgnoreCase(w.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found: " + fromAccountType));
        
        Wallet toWallet = wallets.stream()
                .filter(w -> toAccountType.equalsIgnoreCase(w.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found: " + toAccountType));

        BigDecimal fee = ChargeCalculator.calculateTotalCharge(toAccountType, amount);
        BigDecimal total = amount.add(fee);

        if (fromWallet.getBalance().compareTo(total) < 0) {
            throw new IllegalStateException("Insufficient balance. Required: " + total + ", available: " + fromWallet.getBalance());
        }

        BigDecimal fromPrev = fromWallet.getBalance();
        BigDecimal toPrev = toWallet.getBalance();
        
        fromWallet.setBalance(fromPrev.subtract(total));
        toWallet.setBalance(toPrev.add(amount));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setNetAmount(amount);
        tx.setFromAccount(fromAccountType);
        tx.setToAccount(toAccountType);
        tx.setDate(LocalDateTime.now());
        tx.setWallet(fromWallet);
        tx.setCurrency(fromWallet.getCurrency());
        tx.setSyncStatus("SYNCED");
        tx.setStatus(TransactionStatus.APPROVED);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        transactionRepository.save(tx);

        auditService.logEntity(currentUser.getUsername(), "transactions", tx.getId(), "TRANSFER",
                fromAccountType + ":" + fromPrev + " -> " + toAccountType + ":" + toPrev,
                fromAccountType + ":" + fromWallet.getBalance() + " -> " + toAccountType + ":" + toWallet.getBalance());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
