package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Double getTotalBalanceByOwnerId(@Param("ownerId") Long ownerId);

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
}