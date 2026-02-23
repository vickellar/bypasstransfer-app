package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Find transactions by wallet ID and owner ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId AND t.wallet.owner.id = :ownerId ORDER BY t.date DESC")
    List<Transaction> findByWalletIdAndWalletOwnerId(@Param("walletId") Long walletId, @Param("ownerId") Long ownerId);

    /**
     * Check if transaction exists with given ID and wallet owner ID
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.id = :transactionId AND t.wallet.owner.id = :ownerId")
    boolean existsByIdAndWalletOwnerId(@Param("transactionId") Long transactionId, @Param("ownerId") Long ownerId);

    /**
     * Get transaction by ID and owner ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.wallet.owner.id = :ownerId")
    Optional<Transaction> findByIdAndWalletOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /**
     * Count transactions for a specific user's wallets
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.wallet.owner.id = :ownerId")
    long countByWalletOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Get total transaction amount for a user
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.wallet.owner.id = :ownerId")
    Double getTotalAmountByWalletOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Get total fees for a user
     */
    @Query("SELECT SUM(t.fee) FROM Transaction t WHERE t.wallet.owner.id = :ownerId")
    Double getTotalFeesByWalletOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find transactions by wallet ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId ORDER BY t.date DESC")
    List<Transaction> findByWalletId(@Param("walletId") Long walletId);

    /**
     * Find transactions by wallet
     */
    List<Transaction> findByWallet(com.bypass.bypasstransers.model.Wallet wallet);

    // ===============================
    // COMPANY OVERVIEW QUERIES (SUPERVISORS)
    // ===============================

    /**
     * Get transaction summary by user (for supervisor view)
     */
    @Query("SELECT w.owner.username, COUNT(t), SUM(t.amount), SUM(t.fee) " +
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
}