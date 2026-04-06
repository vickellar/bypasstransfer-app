package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.DailyReconciliation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyReconciliationRepository extends JpaRepository<DailyReconciliation, Long> {

    /** History for a specific wallet, newest first */
    List<DailyReconciliation> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    /** Check if already reconciled this week for a given wallet */
    Optional<DailyReconciliation> findByWeekNumberAndYearAndWalletId(Integer weekNumber, Integer year, Long walletId);

    /** Staff's own reconciliation history */
    List<DailyReconciliation> findByReconciledByOrderByCreatedAtDesc(String reconciledBy);

    /** Find all reconciliations by status (for supervisors) */
    List<DailyReconciliation> findByStatusOrderByCreatedAtDesc(String status);

    /** Find pending + flagged reconciliations (for supervisor notification) */
    @Query("SELECT r FROM DailyReconciliation r WHERE r.status IN ('PENDING', 'FLAGGED') ORDER BY r.createdAt DESC")
    List<DailyReconciliation> findPendingAndFlagged();

    /** Count pending + flagged (for badge count on supervisor dashboard) */
    @Query("SELECT COUNT(r) FROM DailyReconciliation r WHERE r.status IN ('PENDING', 'FLAGGED')")
    long countPendingAndFlagged();

    /** All reconciliations ordered by newest first */
    List<DailyReconciliation> findAllByOrderByCreatedAtDesc();

    /** Find by status and week */
    List<DailyReconciliation> findByStatusAndWeekNumberAndYear(String status, Integer weekNumber, Integer year);

    /** Find all for a specific week */
    List<DailyReconciliation> findByWeekNumberAndYearOrderByCreatedAtDesc(Integer weekNumber, Integer year);
}
