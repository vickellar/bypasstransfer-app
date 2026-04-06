package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
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
     * Legacy method — reconcile total system (kept for backward compatibility)
     */
    @Transactional
    public DailyReconciliation reconcile(LocalDate date, double actualBalance) {
        User currentUser = securityService.getCurrentUser();
        String username = (currentUser != null) ? currentUser.getUsername() : "SYSTEM";

        double systemBalance = walletRepository.findAll()
                .stream()
                .mapToDouble(Wallet::getBalance)
                .sum();

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(date);
        r.setSystemBalance(systemBalance);
        r.setActualBalance(actualBalance);
        r.setDifference(actualBalance - systemBalance);
        r.setAccountName("TOTAL_SYSTEM");
        r.setReconciledBy(username);
        r.setCreatedAt(LocalDateTime.now());
        r.setWeekNumber(getCurrentWeekNumber());
        r.setYear(getCurrentYear());

        // Auto-set status based on difference
        if (Math.abs(r.getDifference()) < 0.01) {
            r.setStatus("APPROVED"); // No discrepancy
        } else {
            r.setStatus("FLAGGED"); // Any non-zero difference is flagged
        }

        return dailyRepo.save(r);
    }

    /**
     * Reconcile a specific wallet with notes and audit trail
     */
    @Transactional
    public DailyReconciliation reconcileWallet(Long walletId, double actualBalance, String notes) {
        User currentUser = securityService.getCurrentUser();
        String username = (currentUser != null) ? currentUser.getUsername() : "SYSTEM";

        // Prevent duplicate reconciliation for the same wallet this week
        int week = getCurrentWeekNumber();
        int year = getCurrentYear();
        Optional<DailyReconciliation> existing = dailyRepo.findByWeekNumberAndYearAndWalletId(week, year, walletId);
        if (existing.isPresent()) {
            throw new IllegalStateException("This wallet has already been reconciled this week (Week " + week + ", " + year + ")");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(LocalDate.now());
        r.setSystemBalance(wallet.getBalance());
        r.setActualBalance(actualBalance);
        r.setDifference(actualBalance - wallet.getBalance());
        r.setWalletId(wallet.getId());
        r.setAccountName(wallet.getAccountType());
        r.setReconciledBy(username);
        r.setNotes(notes);
        r.setCreatedAt(LocalDateTime.now());
        r.setWeekNumber(week);
        r.setYear(year);

        // Auto-set status based on difference
        if (Math.abs(r.getDifference()) < 0.01) {
            r.setStatus("APPROVED"); // No discrepancy — auto-approve
        } else {
            r.setStatus("FLAGGED"); // Any non-zero difference → needs supervisor review
        }

        return dailyRepo.save(r);
    }

    /**
     * Backward-compatible overload (no notes)
     */
    @Transactional
    public DailyReconciliation reconcileWallet(Long walletId, double actualBalance) {
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

    /**
     * Get number of pending/flagged reconciliations (for supervisor badge)
     */
    public long countPendingAndFlagged() {
        return dailyRepo.countPendingAndFlagged();
    }

    /**
     * Get all pending/flagged reconciliations
     */
    public List<DailyReconciliation> getPendingAndFlagged() {
        return dailyRepo.findPendingAndFlagged();
    }

    /**
     * Get reconciliation history for a specific staff member
     */
    public List<DailyReconciliation> getHistoryByStaff(String username) {
        return dailyRepo.findByReconciledByOrderByCreatedAtDesc(username);
    }

    /**
     * Get all reconciliations (for admin)
     */
    public List<DailyReconciliation> getAllReconciliations() {
        return dailyRepo.findAllByOrderByCreatedAtDesc();
    }
}