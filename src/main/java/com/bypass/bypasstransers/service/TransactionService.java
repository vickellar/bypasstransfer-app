package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.dto.TransactionSummary;
import com.bypass.bypasstransers.dto.UserTransactionSummary;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.exception.AccountNotFoundException;
import com.bypass.bypasstransers.exception.InsufficientBalanceException;
import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SmsService smsService;

    private static void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private Account requireAccount(String name) {
        Account acc = accountRepo.findByName(name);
        if (acc == null) {
            throw new AccountNotFoundException("Account not found: " + name);
        }
        return acc;
    }

    /* 1️⃣ RECEIVE MONEY */
    @Transactional(rollbackFor = Exception.class)
    public void receive(String accountName, double amount) {
        validateAmount(amount);
        Account acc = requireAccount(accountName);
        double prevBalance = acc.getBalance();
        acc.setBalance(prevBalance + amount);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.INCOME);
        tx.setAmount(amount);
        tx.setFee(0);
        tx.setToAccount(accountName);
        tx.setDate(LocalDateTime.now());

        accountRepo.save(acc);
        txRepo.save(tx);

        auditService.logEntity(null, "transactions", tx.getId(), "RECEIVE",
                String.valueOf(prevBalance), String.valueOf(acc.getBalance()));
        sendTransactionSms(acc, "Received " + amount + " into " + accountName + ". New balance: " + acc.getBalance());

        checkLowBalance(acc);
    }

    /* 2️⃣ SEND MONEY (EXPENSE) */
    @Transactional(rollbackFor = Exception.class)
    public void send(String accountName, double amount) {
        validateAmount(amount);
        Account acc = requireAccount(accountName);

        double fee = ChargeCalculator.calculateFee(acc, amount);
        double total = amount + fee;

        if (acc.getBalance() < total) {
            throw new InsufficientBalanceException("Insufficient balance. Required: " + total + ", available: " + acc.getBalance());
        }

        double prevBalance = acc.getBalance();
        acc.setBalance(prevBalance - total);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setFromAccount(accountName);
        tx.setDate(LocalDateTime.now());

        accountRepo.save(acc);
        txRepo.save(tx);

        auditService.logEntity(null, "transactions", tx.getId(), "SEND",
                String.valueOf(prevBalance), String.valueOf(acc.getBalance()));
        sendTransactionSms(acc, "Sent " + amount + " (fee: " + fee + ") from " + accountName + ". New balance: " + acc.getBalance());

        checkLowBalance(acc);
    }

    /**
     * Get transactions for current user with role-based filtering
     * - Staff: only their own transactions
     * - Supervisor/Admin: all transactions
     */
    public List<Transaction> findAllForCurrentUser() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only see their own transactions
        if (securityService.isStaffOnly()) {
            return txRepo.findByWalletOwnerId(currentUser.getId());
        }

        // Supervisors and above can see all transactions
        return txRepo.findAll();
    }

    /**
     * Get all transactions - only for supervisors and above
     */
    public List<Transaction> findAll() {
        if (!securityService.isSupervisorOrAbove()) {
            throw new AccessDeniedException("Insufficient privileges to view all transactions");
        }
        return txRepo.findAll();
    }

    /**
     * Get transaction by ID - enforces user isolation
     */
    public Optional<Transaction> findById(Long id) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only access their own transactions
        if (securityService.isStaffOnly()) {
            return txRepo.findByIdAndWalletOwnerId(id, currentUser.getId());
        }

        // Supervisors and above can access any transaction
        return txRepo.findById(id);
    }

    /**
     * Get transactions for a specific wallet - enforces user isolation
     */
    public List<Transaction> findByWalletId(Long walletId) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only access their own wallet transactions
        if (securityService.isStaffOnly()) {
            return txRepo.findByWalletIdAndWalletOwnerId(walletId, currentUser.getId());
        }

        // Supervisors and above can access any wallet transactions
        return txRepo.findByWalletId(walletId);
    }

    /**
     * Get transaction summary for current user
     */
    public TransactionSummary getCurrentUserSummary() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Long userId = currentUser.getId();
        long count = txRepo.countByWalletOwnerId(userId);
        Double totalAmount = txRepo.getTotalAmountByWalletOwnerId(userId);
        Double totalFees = txRepo.getTotalFeesByWalletOwnerId(userId);

        TransactionSummary summary = new TransactionSummary();
        summary.setTransactionCount(count);
        summary.setTotalAmount(totalAmount != null ? totalAmount : 0.0);
        summary.setTotalFees(totalFees != null ? totalFees : 0.0);
        return summary;
    }

    /**
     * Get company-wide transaction summary - only for supervisors
     */
    public List<UserTransactionSummary> getCompanyTransactionSummary() {
        if (!securityService.isSupervisorOrAbove()) {
            throw new AccessDeniedException("Insufficient privileges");
        }

        List<Object[]> results = txRepo.getTransactionSummaryByUser();
        List<UserTransactionSummary> summaries = new ArrayList<>();

        for (Object[] row : results) {
            UserTransactionSummary summary = new UserTransactionSummary();
            summary.setUsername((String) row[0]);
            summary.setTransactionCount((Long) row[1]);
            summary.setTotalAmount((Double) row[2]);
            summary.setTotalFees((Double) row[3]);
            summaries.add(summary);
        }

        return summaries;
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction save(Transaction tx) {
        tx.setTransactionDate(LocalDateTime.now());
        tx.setSyncStatus("SYNC_PENDING");
        return txRepo.save(tx);
    }

    /* 3️⃣ TRANSFER BETWEEN ACCOUNTS */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String fromName, String toName, double amount) {
        validateAmount(amount);
        Account from = requireAccount(fromName);
        Account to = requireAccount(toName);

        double fee = ChargeCalculator.calculateFee(from, amount);
        double total = amount + fee;

        if (from.getBalance() < total) {
            throw new InsufficientBalanceException("Insufficient balance. Required: " + total + ", available: " + from.getBalance());
        }

        double fromPrev = from.getBalance();
        double toPrev = to.getBalance();
        from.setBalance(fromPrev - total);
        to.setBalance(toPrev + amount);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setFromAccount(fromName);
        tx.setToAccount(toName);
        tx.setDate(LocalDateTime.now());

        accountRepo.save(from);
        accountRepo.save(to);
        txRepo.save(tx);

        auditService.logEntity(null, "transactions", tx.getId(), "TRANSFER",
                fromName + ":" + fromPrev + " -> " + toName + ":" + toPrev,
                fromName + ":" + from.getBalance() + " -> " + toName + ":" + to.getBalance());
        sendTransactionSms(from, "Transfer of " + amount + " (fee: " + fee + ") from " + fromName + " to " + toName);
        sendTransactionSms(to, "Received " + amount + " from " + fromName + " into " + toName + ". New balance: " + to.getBalance());

        checkLowBalance(from);
        checkLowBalance(to);
    }

    private void sendTransactionSms(Account account, String message) {
        try {
            User owner = account.getOwner();
            if (owner != null && owner.getPhoneNumber() != null && !owner.getPhoneNumber().isBlank()) {
                smsService.sendAlert(owner, message);
            }
        } catch (Exception e) {
            // Log but don't fail the transaction
            org.slf4j.LoggerFactory.getLogger(TransactionService.class).warn("SMS notification failed: {}", e.getMessage());
        }
    }

    /* 4️⃣ PROFIT & FEES */
    public double totalFees() {
        return txRepo.findAll()
                .stream()
                .mapToDouble(Transaction::getFee)
                .sum();
    }

    private void checkLowBalance(Account account) {
        if (account.getBalance() < account.getLowBalanceThreshold()
                && !account.isLowBalanceAlertSent()) {

            alertService.notifyLowBalance(account);
            // persist the flag change
            accountRepo.save(account);
        }
    }

}