package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private SecurityService securityService;

    /**
     * Get all wallets for the current user
     * - Staff: returns only their wallets
     * - Supervisor/Admin: can call this but typically use getAllWalletsForSupervisor()
     */
    public List<Wallet> getCurrentUserWallets() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only see their own wallets
        if (securityService.isStaffOnly()) {
            return walletRepository.findByOwnerId(currentUser.getId());
        }

        // Supervisors and above can see all wallets
        return walletRepository.findAll();
    }

    /**
     * Get wallet by ID - enforces user isolation
     */
    public Optional<Wallet> getWalletById(Long id) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only access their own wallets
        if (securityService.isStaffOnly()) {
            return walletRepository.findByIdAndOwnerId(id, currentUser.getId());
        }

        // Supervisors and above can access any wallet
        return walletRepository.findById(id);
    }

    /**
     * Get all wallets - only for supervisors and above
     */
    public List<Wallet> getAllWallets() {
        if (!securityService.isSupervisorOrAbove()) {
            throw new AccessDeniedException("Insufficient privileges to view all wallets");
        }
        return walletRepository.findAll();
    }

    /**
     * Get wallets for a specific user - only for supervisors and above
     */
    public List<Wallet> getWalletsByUserId(Long userId) {
        if (!securityService.isSupervisorOrAbove()) {
            throw new AccessDeniedException("Insufficient privileges to view other user's wallets");
        }
        return walletRepository.findByOwnerId(userId);
    }

    /**
     * Get total balance for current user
     */
    public Double getCurrentUserTotalBalance() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Double total = walletRepository.getTotalBalanceByOwnerId(currentUser.getId());
        return total != null ? total : 0.0;
    }

    /**
     * Get total balance for all users - only for supervisors
     */
    public Double getCompanyTotalBalance() {
        if (!securityService.isSupervisorOrAbove()) {
            throw new AccessDeniedException("Insufficient privileges");
        }

        return walletRepository.findAll().stream()
                .mapToDouble(Wallet::getBalance)
                .sum();
    }

    /**
     * Count wallets for current user
     */
    public long countCurrentUserWallets() {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return walletRepository.countByOwnerId(currentUser.getId());
    }

    /**
     * Save wallet - ensures staff can only create wallets for themselves
     */
    public Wallet saveWallet(Wallet wallet) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only create wallets for themselves
        if (securityService.isStaffOnly()) {
            wallet.setOwner(currentUser);
        }

        return walletRepository.save(wallet);
    }

    /**
     * Delete wallet - enforces ownership
     */
    public void deleteWallet(Long id) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        // Staff can only delete their own wallets
        if (securityService.isStaffOnly()) {
            Optional<Wallet> wallet = walletRepository.findByIdAndOwnerId(id, currentUser.getId());
            if (wallet.isEmpty()) {
                throw new AccessDeniedException("Wallet not found or access denied");
            }
        }

        walletRepository.deleteById(id);
    }

    /**
     * Check if current user can access a specific wallet
     */
    public boolean canAccessWallet(Long walletId) {
        return securityService.canAccessWallet(walletId);
    }
}
