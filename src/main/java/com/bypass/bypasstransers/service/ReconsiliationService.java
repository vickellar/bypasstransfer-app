package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconsiliationService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private DailyReconciliationRepository dailyRepo;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SecurityService securityService;

    /**
     * Get the current ISO week number
     */
    private int getCurrentWeekNumber() {
        return LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
    }

    /**
     * Get the current year
     */
    private int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * Check if a wallet has already been reconciled this week
     */
    public boolean isAlreadyReconciledThisWeek(Long walletId) {
        int week = getCurrentWeekNumber();
        int year = getCurrentYear();
        return dailyRepo.findByWeekNumberAndYearAndWalletId(week, year, walletId).isPresent();
    }

    /**
     * Get existing reconciliation for this week if it exists
     */
    public Optional<DailyReconciliation> getThisWeeksReconciliation(Long walletId) {
        int week = getCurrentWeekNumber();
        int year = getCurrentYear();
        return dailyRepo.findByWeekNumberAndYearAndWalletId(week, year, walletId);
    }

    /**
     * Legacy method — reconcile total system
     */
    @Transactional
    public DailyReconciliation reconcile(LocalDate date, BigDecimal actualBalance) {
        User currentUser = securityService.getCurrentUser();
        String username = (currentUser != null) ? currentUser.getUsername() : "SYSTEM";

        BigDecimal systemBalance = walletRepository.findAll()
                .stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(date);
        r.setSystemBalance(systemBalance);
        r.setActualBalance(actualBalance);
        r.setDifference(actualBalance.subtract(systemBalance));
        r.setAccountName("TOTAL_SYSTEM");
        r.setReconciledBy(username);
        r.setCreatedAt(LocalDateTime.now());
        r.setWeekNumber(getCurrentWeekNumber());
        r.setYear(getCurrentYear());

        // Auto-set status based on difference (discrepancy threshold: 0.01)
        if (r.getDifference().abs().compareTo(new BigDecimal("0.01")) < 0) {
            r.setStatus("APPROVED"); 
        } else {
            r.setStatus("FLAGGED");
        }

        return dailyRepo.save(r);
    }

    /**
     * Reconcile a specific wallet with notes and audit trail.
     * Compares the actual (physical) balance against the system wallet balance
     * and also cross-references the sum of transactions for the current week
     * to detect discrepancies between recorded transactions and wallet state.
     */
    @Transactional
    public DailyReconciliation reconcileWallet(Long walletId, BigDecimal actualBalance, String notes) {
        User currentUser = securityService.getCurrentUser();
        String username = (currentUser != null) ? currentUser.getUsername() : "SYSTEM";

        int week = getCurrentWeekNumber();
        int year = getCurrentYear();
        Optional<DailyReconciliation> existing = dailyRepo.findByWeekNumberAndYearAndWalletId(week, year, walletId);
        if (existing.isPresent()) {
            throw new IllegalStateException("This wallet has already been reconciled this week (Week " + week + ", " + year + ")");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

        // Cross-reference: sum transactions linked to this wallet for the current week
        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        BigDecimal txTotal = transactionRepository.findAll().stream()
                .filter(tx -> tx.getWallet() != null && tx.getWallet().getId().equals(walletId))
                .filter(tx -> tx.getDate() != null && tx.getDate().isAfter(weekStartTime))
                .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = actualBalance.subtract(currentBalance);

        // Build detailed notes with transaction cross-reference
        StringBuilder reconNotes = new StringBuilder();
        if (notes != null && !notes.isBlank()) {
            reconNotes.append(notes).append(" | ");
        }
        reconNotes.append("Week TX total: ").append(txTotal.toPlainString());
        reconNotes.append(", Wallet balance: ").append(currentBalance.toPlainString());
        reconNotes.append(", Actual: ").append(actualBalance.toPlainString());
        reconNotes.append(", Diff: ").append(difference.toPlainString());

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(LocalDate.now());
        r.setSystemBalance(currentBalance);
        r.setActualBalance(actualBalance);
        r.setDifference(difference);
        r.setWalletId(wallet.getId());
        r.setAccountName(wallet.getAccountType());
        r.setReconciledBy(username);
        r.setNotes(reconNotes.toString());
        r.setCreatedAt(LocalDateTime.now());
        r.setWeekNumber(week);
        r.setYear(year);

        // Auto-set status: APPROVED if difference is negligible, FLAGGED otherwise
        if (difference.abs().compareTo(new BigDecimal("0.01")) < 0) {
            r.setStatus("APPROVED");
        } else {
            r.setStatus("FLAGGED");
        }

        // Update the wallet balance to the actual (reconciled) balance
        wallet.setBalance(actualBalance);
        walletRepository.save(wallet);

        return dailyRepo.save(r);
    }

    /**
     * Backward-compatible overload (no notes)
     */
    @Transactional
    public DailyReconciliation reconcileWallet(Long walletId, BigDecimal actualBalance) {
        return reconcileWallet(walletId, actualBalance, null);
    }

    /**
     * Supervisor: approve a reconciliation
     */
    @Transactional
    public DailyReconciliation approve(Long reconciliationId, String reviewNotes) {
        User currentUser = securityService.getCurrentUser();
        String username = (currentUser != null) ? currentUser.getUsername() : "SYSTEM";

        DailyReconciliation r = dailyRepo.findById(reconciliationId)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));

        r.setStatus("APPROVED");
        r.setReviewedBy(username);
        r.setReviewNotes(reviewNotes);
        r.setReviewedAt(LocalDateTime.now());

        return dailyRepo.save(r);
    }

    /**
     * Supervisor: flag/reject a reconciliation
     */
    @Transactional
    public DailyReconciliation flag(Long reconciliationId, String reviewNotes) {
        User currentUser = securityService.getCurrentUser();
        String username = (currentUser != null) ? currentUser.getUsername() : "SYSTEM";

        DailyReconciliation r = dailyRepo.findById(reconciliationId)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));

        r.setStatus("FLAGGED");
        r.setReviewedBy(username);
        r.setReviewNotes(reviewNotes);
        r.setReviewedAt(LocalDateTime.now());

        return dailyRepo.save(r);
    }

    public long countPendingAndFlagged() {
        return dailyRepo.countPendingAndFlagged();
    }

    public List<DailyReconciliation> getPendingAndFlagged() {
        return dailyRepo.findPendingAndFlagged();
    }

    public List<DailyReconciliation> getHistoryByStaff(String username) {
        return dailyRepo.findByReconciledByOrderByCreatedAtDesc(username);
    }

    public List<DailyReconciliation> getAllReconciliations() {
        return dailyRepo.findAllByOrderByCreatedAtDesc();
    }
}