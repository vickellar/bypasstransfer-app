package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Get the currently authenticated user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsernameIgnoreCase(username);
    }

    /**
     * Check if current user can access a specific wallet
     * - Staff can only access their own wallets
     * - Supervisors and above can access any wallet
     */
    public boolean canAccessWallet(Long walletId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Supervisors and above can access any wallet
        if (isSupervisorOrAbove()) {
            return true;
        }

        // Staff can only access their own wallets
        return walletRepository.existsByIdAndOwnerId(walletId, currentUser.getId());
    }

    /**
     * Check if current user can access a specific transaction
     * - Staff can only access transactions from their own wallets
     * - Supervisors and above can access any transaction
     */
    public boolean canAccessTransaction(Long transactionId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Supervisors and above can access any transaction
        if (isSupervisorOrAbove()) {
            return true;
        }

        // Staff can only access transactions from their own wallets
        return transactionRepository.existsByIdAndWalletOwnerId(transactionId, currentUser.getId());
    }

    /**
     * Check if current user is a supervisor or has higher privileges
     */
    public boolean isSupervisorOrAbove() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        Role role = currentUser.getRole();
        return role == Role.SUPERVISOR || role == Role.SUPER_ADMIN || role == Role.ADMIN;
    }

    /**
     * Check if current user is a super admin
     */
    public boolean isSuperAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole() == Role.SUPER_ADMIN;
    }

    /**
     * Get the ID of the current user
     */
    public Long getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * Check if user has staff role only (not supervisor or admin)
     */
    public boolean isStaffOnly() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole() == Role.STAFF;
    }
}
