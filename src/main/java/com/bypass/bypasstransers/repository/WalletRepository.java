package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Find all wallets belonging to a specific user
     */
    List<Wallet> findByOwnerId(Long ownerId);

    /**
     * Find wallet by ID and owner ID - for staff access verification
     */
    Optional<Wallet> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Check if wallet exists with given ID and owner ID
     */
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Count wallets for a specific user
     */
    long countByOwnerId(Long ownerId);

    /**
     * Get total balance across all wallets for a user
     */
    @Query("SELECT SUM(w.balance) FROM Wallet w WHERE w.owner.id = :ownerId")
    BigDecimal getTotalBalanceByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find wallets by currency for a specific user
     */
    List<Wallet> findByOwnerIdAndCurrency(Long ownerId, String currency);

    /**
     * Find wallets by owner
     */
    List<Wallet> findByOwner(com.bypass.bypasstransers.model.User owner);

    /**
     * Find wallets by account type
     */
    List<Wallet> findByAccountType(String accountType);
    
    /**
     * Find wallets by owner ID and account type
     */
    List<Wallet> findByOwnerIdAndAccountType(Long ownerId, String accountType);

    /**
     * Find wallets by branch
     */
    List<Wallet> findByBranchId(Long branchId);

    /**
     * Find wallets by branch and currency
     */
    List<Wallet> findByBranchIdAndCurrency(Long branchId, String currency);

    /**
     * Count wallets by branch
     */
    long countByBranchId(Long branchId);

    /**
     * Find all wallets belonging to a specific user OR assigned to their branch
     */
    @Query("SELECT w FROM Wallet w WHERE w.owner.id = :ownerId OR (w.branch IS NOT NULL AND w.branch.id = :branchId)")
    List<Wallet> findByOwnerIdOrBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    /**
     * Get total balance across all wallets for a user OR their branch
     */
    @Query("SELECT SUM(w.balance) FROM Wallet w WHERE w.owner.id = :ownerId OR (w.branch IS NOT NULL AND w.branch.id = :branchId)")
    BigDecimal getTotalBalanceByOwnerIdOrBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);

    /**
     * Count wallets belonging to a user OR assigned to their branch
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.owner.id = :ownerId OR (w.branch IS NOT NULL AND w.branch.id = :branchId)")
    long countByOwnerIdOrBranchId(@Param("ownerId") Long ownerId, @Param("branchId") Long branchId);
}