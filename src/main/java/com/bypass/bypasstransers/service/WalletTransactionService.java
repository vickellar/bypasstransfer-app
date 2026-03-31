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
    public void receive(String accountType, double amount) {
        validateAmount(amount);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not authenticated");
        }

        // Find wallet by account type and current user
        List<Wallet> wallets = walletRepository.findByOwnerId(currentUser.getId());
        System.out.println("[RECEIVE] Looking for wallet with accountType: '" + accountType + "'");
        System.out.println("[RECEIVE] User: " + currentUser.getUsername() + ", Available wallets: " + wallets.size());
        for (Wallet w : wallets) {
            System.out.println("  - Wallet: '" + w.getAccountType() + "' (ID: " + w.getId() + ", Balance: " + w.getBalance() + ")");
        }
        
        Wallet wallet = wallets.stream()
                .filter(w -> accountType != null && accountType.equalsIgnoreCase(w.getAccountType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: '" + accountType + "'. Available: " + 
                    wallets.stream().map(Wallet::getAccountType).toList()));

        System.out.println("[RECEIVE] Found wallet ID: " + wallet.getId() + ", verifying in database...");
        
        // Verify wallet exists in database by fetching it fresh
        Wallet dbWallet = walletRepository.findById(wallet.getId()).orElse(null);
        if (dbWallet == null) {
            System.out.println("[RECEIVE] ERROR: Wallet ID " + wallet.getId() + " not found in database!");
            throw new IllegalStateException("Wallet not found in database. Please contact admin.");
        }
        System.out.println("[RECEIVE] Database wallet confirmed: ID=" + dbWallet.getId() + ", Type=" + dbWallet.getAccountType());

        double prevBalance = dbWallet.getBalance();
        dbWallet.setBalance(prevBalance + amount);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.INCOME);
        tx.setAmount(amount);
        tx.setFee(0.0);
        tx.setNetAmount(amount); // receiver credited with 'amount'
        tx.setToAccount(accountType);
        tx.setDate(LocalDateTime.now());
        tx.setWallet(dbWallet);
        tx.setCurrency(dbWallet.getCurrency());
        tx.setSyncStatus("SYNCED"); // Online transactions are already synced
        tx.setStatus(TransactionStatus.APPROVED);

        walletRepository.save(dbWallet);
        transactionRepository.save(tx);
        
        System.out.println("[RECEIVE] Success! Wallet: " + wallet.getAccountType() + ", Old: " + prevBalance + ", New: " + wallet.getBalance());

        auditService.logEntity(currentUser.getUsername(), "transactions", tx.getId(), "RECEIVE",
                String.valueOf(prevBalance), String.valueOf(wallet.getBalance()));
    }

    /**
     * Send money (Cash Out) - deducts from wallet balance
     */
    @Transactional(rollbackFor = Exception.class)
    public void send(String accountType, double amount) {
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

        // Total charge = 5% company profit + provider fee (based on selected method).
        double fee = ChargeCalculator.calculateTotalCharge(accountType, amount);
        double total = amount + fee;

        if (wallet.getBalance() < total) {
            throw new IllegalStateException("Insufficient balance. Required: " + total + ", available: " + wallet.getBalance());
        }

        double prevBalance = wallet.getBalance();
        wallet.setBalance(prevBalance - total);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setNetAmount(amount); // receiver gets 'amount'; fee is charged on top
        tx.setFromAccount(accountType);
        tx.setDate(LocalDateTime.now());
        tx.setWallet(wallet);
        tx.setCurrency(wallet.getCurrency());
        tx.setSyncStatus("SYNCED"); // Online transactions are already synced
        tx.setStatus(TransactionStatus.APPROVED);

        walletRepository.save(wallet);
        transactionRepository.save(tx);

        auditService.logEntity(currentUser.getUsername(), "transactions", tx.getId(), "SEND",
                String.valueOf(prevBalance), String.valueOf(wallet.getBalance()));
    }

    /**
     * Transfer between wallets
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String fromAccountType, String toAccountType, double amount) {
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

        // Provider fee depends on the chosen receiving method (toAccountType).
        double fee = ChargeCalculator.calculateTotalCharge(toAccountType, amount);
        double total = amount + fee;

        if (fromWallet.getBalance() < total) {
            throw new IllegalStateException("Insufficient balance. Required: " + total + ", available: " + fromWallet.getBalance());
        }

        double fromPrev = fromWallet.getBalance();
        double toPrev = toWallet.getBalance();
        
        fromWallet.setBalance(fromPrev - total);
        toWallet.setBalance(toPrev + amount);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setNetAmount(amount); // receiver gets 'amount'
        tx.setFromAccount(fromAccountType);
        tx.setToAccount(toAccountType);
        tx.setDate(LocalDateTime.now());
        tx.setWallet(fromWallet);
        tx.setCurrency(fromWallet.getCurrency());
        tx.setSyncStatus("SYNCED"); // Online transactions are already synced
        tx.setStatus(TransactionStatus.APPROVED);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        transactionRepository.save(tx);

        auditService.logEntity(currentUser.getUsername(), "transactions", tx.getId(), "TRANSFER",
                fromAccountType + ":" + fromPrev + " -> " + toAccountType + ":" + toPrev,
                fromAccountType + ":" + fromWallet.getBalance() + " -> " + toAccountType + ":" + toWallet.getBalance());
    }

    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    // Fee calculation is handled by ChargeCalculator (5% profit + provider fee).
}
