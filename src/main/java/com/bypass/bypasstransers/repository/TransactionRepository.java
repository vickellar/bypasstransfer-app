package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySyncStatus(String status);

    @Query("SELECT t.fromAccount, SUM(t.amount) FROM Transaction t GROUP BY t.fromAccount")
    Iterable<Object[]> volumeByAccount();

    @Query("SELECT function('date', t.date), SUM(t.amount) FROM Transaction t GROUP BY function('date', t.date) ORDER BY function('date', t.date) DESC")
    Iterable<Object[]> dailyVolume();

    @Query("SELECT t.fromAccount, SUM(t.fee) FROM Transaction t GROUP BY t.fromAccount")
    Iterable<Object[]> feesByAccount();

    // ===============================
    // USER-ISOLATED QUERIES FOR STAFF
    // ===============================

    /**
     * Find all transactions for wallets belonging to a specific user
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.owner.id = :ownerId ORDER BY t.date DESC")
    List<Transaction> findByWalletOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find all transactions for wallets belonging to a specific user OR assigned to their branch
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.owner.id = :ownerId OR (t.wallet.branch IS NOT NULL AND t.wallet.branch.id = :branchId) ORDER BY t.date DESC")
    List<Transaction> findByWalletOwnerIdOrWalletBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    /**
     * Find transactions by wallet ID and owner ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId AND t.wallet.owner.id = :ownerId ORDER BY t.date DESC")
    List<Transaction> findByWalletIdAndWalletOwnerId(@Param("walletId") Long walletId, @Param("ownerId") Long ownerId);

    /**
     * Find transaction by ID and owner ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.wallet.owner.id = :ownerId")
    Optional<Transaction> findByIdAndWalletOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /**
     * Check if transaction exists with given ID and wallet owner ID
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.id = :id AND t.wallet.owner.id = :ownerId")
    boolean existsByIdAndWalletOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /**
     * Check if transaction exists with given ID and wallet owner ID or branch ID
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.id = :transactionId AND (t.wallet.owner.id = :ownerId OR (t.wallet.branch IS NOT NULL AND t.wallet.branch.id = :branchId))")
    boolean existsByIdAndWalletOwnerIdOrWalletBranchId(@Param("transactionId") Long transactionId, @Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    /**
     * Get transaction by ID and owner ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND (t.wallet.owner.id = :ownerId OR (t.wallet.branch IS NOT NULL AND t.wallet.branch.id = :branchId))")
    Optional<Transaction> findByIdAndWalletOwnerIdOrWalletBranchId(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    /**
     * Count transactions for a specific user's wallets
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.wallet.owner.id = :ownerId OR (t.wallet.branch IS NOT NULL AND t.wallet.branch.id = :branchId)")
    long countByWalletOwnerIdOrWalletBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.wallet.owner.id = :ownerId OR (t.wallet.branch IS NOT NULL AND t.wallet.branch.id = :branchId)")
    BigDecimal getTotalAmountByWalletOwnerIdOrWalletBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    @Query("SELECT SUM(t.fee) FROM Transaction t WHERE t.wallet.owner.id = :ownerId OR (t.wallet.branch IS NOT NULL AND t.wallet.branch.id = :branchId)")
    BigDecimal getTotalFeesByWalletOwnerIdOrWalletBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.wallet.owner.id = :ownerId")
    long countByWalletOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.wallet.owner.id = :ownerId")
    BigDecimal getTotalAmountByWalletOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT SUM(t.fee) FROM Transaction t WHERE t.wallet.owner.id = :ownerId")
    BigDecimal getTotalFeesByWalletOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find transactions by wallet ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId ORDER BY t.date DESC")
    List<Transaction> findByWalletId(@Param("walletId") Long walletId);

    /**
     * Find transactions by wallet
     */
    List<Transaction> findByWallet(com.bypass.bypasstransers.model.Wallet wallet);

    /**
     * Find transactions within a date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    List<Transaction> findByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ===============================
    // COMPANY OVERVIEW QUERIES (SUPERVISORS)
    // ===============================

    /**
     * Get transaction summary by user (for supervisor view)
     */
    @Query("SELECT w.owner.id, w.owner.username, COUNT(t), SUM(t.amount), SUM(t.fee) " +
           "FROM Transaction t JOIN t.wallet w " +
           "GROUP BY w.owner.id, w.owner.username")
    List<Object[]> getTransactionSummaryByUser();

    /**
     * Get daily volume for all users
     */
    @Query("SELECT function('date', t.date), SUM(t.amount), COUNT(t) " +
           "FROM Transaction t " +
           "GROUP BY function('date', t.date) " +
           "ORDER BY function('date', t.date) DESC")
    List<Object[]> getDailyVolumeAllUsers();
    
    /**
     * Find transactions by creator username (for admin audit)
     */
    List<Transaction> findByCreatedBy(String createdBy);
}