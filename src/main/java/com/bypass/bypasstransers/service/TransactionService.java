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
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private WalletRepository walletRepository;

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
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
    public void receive(String accountName, BigDecimal amount) {
        validateAmount(amount);
        Account acc = requireAccount(accountName);
        BigDecimal prevBalance = acc.getBalance();
        acc.setBalance(prevBalance.add(amount));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.INCOME);
        tx.setAmount(amount);
        tx.setFee(BigDecimal.ZERO);
        tx.setToAccount(accountName);
        tx.setDate(LocalDateTime.now());
        tx.setSyncStatus("SYNCED");

        accountRepo.save(acc);
        txRepo.save(tx);

        auditService.logEntity(null, "transactions", tx.getId(), "RECEIVE",
                prevBalance.toString(), acc.getBalance().toString());
        sendTransactionSms(acc, "Received " + amount + " into " + accountName + ". New balance: " + acc.getBalance());

        checkLowBalance(acc);
    }

    /* 2️⃣ SEND MONEY (EXPENSE) */
    @Transactional(rollbackFor = Exception.class)
    public void send(String accountName, BigDecimal amount) {
        validateAmount(amount);
        Account acc = requireAccount(accountName);

        BigDecimal fee = ChargeCalculator.calculateFee(acc, amount);
        BigDecimal total = amount.add(fee);

        if (acc.getBalance().compareTo(total) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Required: " + total + ", available: " + acc.getBalance());
        }

        BigDecimal prevBalance = acc.getBalance();
        acc.setBalance(prevBalance.subtract(total));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setFromAccount(accountName);
        tx.setDate(LocalDateTime.now());
        tx.setSyncStatus("SYNCED");

        accountRepo.save(acc);
        txRepo.save(tx);

        auditService.logEntity(null, "transactions", tx.getId(), "SEND",
                prevBalance.toString(), acc.getBalance().toString());
        sendTransactionSms(acc, "Sent " + amount + " (fee: " + fee + ") from " + accountName + ". New balance: " + acc.getBalance());

        checkLowBalance(acc);
    }

    /**
     * Get transactions for current user with role-based filtering
     */
    public List<Transaction> findAllForCurrentUser() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        if (securityService.isStaffOnly()) {
            Long branchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (branchId != null) {
                return txRepo.findByWalletOwnerIdOrWalletBranchId(currentUser.getId(), branchId);
            }
            return txRepo.findByWalletOwnerId(currentUser.getId());
        }

        return txRepo.findAll();
    }

    public List<Transaction> findAll() {
        if (!securityService.isSupervisorOrAbove()) {
            throw new AccessDeniedException("Insufficient privileges to view all transactions");
        }
        return txRepo.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        if (securityService.isStaffOnly()) {
            Long branchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (branchId != null) {
                return txRepo.findByIdAndWalletOwnerIdOrWalletBranchId(id, currentUser.getId(), branchId);
            }
            return txRepo.findByIdAndWalletOwnerId(id, currentUser.getId());
        }

        return txRepo.findById(id);
    }

    public List<Transaction> findByWalletId(Long walletId) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        if (securityService.isStaffOnly()) {
            Long branchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (branchId != null) {
                return txRepo.findByWalletId(walletId);
            }
            return txRepo.findByWalletIdAndWalletOwnerId(walletId, currentUser.getId());
        }

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
        long count;
        BigDecimal totalAmount;
        BigDecimal totalFees;

        if (securityService.isStaffOnly()) {
            Long branchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (branchId != null) {
                count = txRepo.countByWalletOwnerIdOrWalletBranchId(userId, branchId);
                totalAmount = txRepo.getTotalAmountByWalletOwnerIdOrWalletBranchId(userId, branchId);
                totalFees = txRepo.getTotalFeesByWalletOwnerIdOrWalletBranchId(userId, branchId);
            } else {
                count = txRepo.countByWalletOwnerId(userId);
                totalAmount = txRepo.getTotalAmountByWalletOwnerId(userId);
                totalFees = txRepo.getTotalFeesByWalletOwnerId(userId);
            }
        } else {
            count = txRepo.countByWalletOwnerId(userId);
            totalAmount = txRepo.getTotalAmountByWalletOwnerId(userId);
            totalFees = txRepo.getTotalFeesByWalletOwnerId(userId);
        }

        TransactionSummary summary = new TransactionSummary();
        summary.setTransactionCount(count);
        summary.setTotalAmount(totalAmount != null ? totalAmount.doubleValue() : 0.0);
        summary.setTotalFees(totalFees != null ? totalFees.doubleValue() : 0.0);
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
            Long ownerId = (Long) row[0];
            summary.setUserId(ownerId);
            summary.setUsername((String) row[1]);
            summary.setTransactionCount((Long) row[2]);
            summary.setTotalAmount(row[3] != null ? ((BigDecimal) row[3]).doubleValue() : 0.0);
            summary.setTotalFees(row[4] != null ? ((BigDecimal) row[4]).doubleValue() : 0.0);
            BigDecimal walletBalance = walletRepository.getTotalBalanceByOwnerId(ownerId);
            summary.setWalletBalance(walletBalance != null ? walletBalance.doubleValue() : 0.0);
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
    public void transfer(String fromName, String toName, BigDecimal amount) {
        validateAmount(amount);
        Account from = requireAccount(fromName);
        Account to = requireAccount(toName);

        BigDecimal fee = ChargeCalculator.calculateFee(from, amount);
        BigDecimal total = amount.add(fee);

        if (from.getBalance().compareTo(total) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Required: " + total + ", available: " + from.getBalance());
        }

        BigDecimal fromPrev = from.getBalance();
        BigDecimal toPrev = to.getBalance();
        from.setBalance(fromPrev.subtract(total));
        to.setBalance(toPrev.add(amount));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setFromAccount(fromName);
        tx.setToAccount(toName);
        tx.setDate(LocalDateTime.now());
        tx.setSyncStatus("SYNCED");

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
            org.slf4j.LoggerFactory.getLogger(TransactionService.class).warn("SMS notification failed: {}", e.getMessage());
        }
    }

    /* 4️⃣ PROFIT & FEES */
    public BigDecimal totalFees() {
        return txRepo.findAll()
                .stream()
                .map(Transaction::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void checkLowBalance(Account account) {
        if (account.getBalance().compareTo(account.getLowBalanceThreshold()) < 0
                && !account.isLowBalanceAlertSent()) {

            alertService.notifyLowBalance(account);
            accountRepo.save(account);
        }
    }
}