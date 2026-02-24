package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.Currency;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class UserProvisioningService {

    @Autowired
    private WalletRepository walletRepository;

    /**
     * The three default account types every user should have
     */
    public static final List<DefaultAccount> DEFAULT_ACCOUNTS = Arrays.asList(
        new DefaultAccount("Mukuru", "USD", "International transfers"),
        new DefaultAccount("Econet", "USD", "Mobile money - Econet"),
        new DefaultAccount("Innbucks", "USD", "Innbucks transfers")
    );

    /**
     * Create the three default wallets for a new user
     * This should be called after a new user is created
     */
    @Transactional
    public void createDefaultWalletsForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be saved before creating wallets");
        }

        for (DefaultAccount defAcc : DEFAULT_ACCOUNTS) {
            // Check if wallet already exists for this user by account type
            List<Wallet> existingWallets = walletRepository.findByOwnerIdAndAccountType(user.getId(), defAcc.name);
            boolean exists = !existingWallets.isEmpty();
            
            if (!exists) {
                Wallet wallet = new Wallet();
                wallet.setOwner(user);
                wallet.setAccountType(defAcc.name);
                wallet.setCurrency(Currency.valueOf(defAcc.currency));
                wallet.setBalance(0.0);
                wallet.setLocked(false);
                walletRepository.save(wallet);
                System.out.println("[WALLET CREATION] Created wallet " + defAcc.name + " for user " + user.getUsername());
            } else {
                System.out.println("[WALLET CREATION] Wallet " + defAcc.name + " already exists for user " + user.getUsername());
            }
        }
    }

    /**
     * Check if user has all default wallets
     */
    public boolean hasDefaultWallets(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        
        List<Wallet> userWallets = walletRepository.findByOwnerId(user.getId());
        
        for (DefaultAccount defAcc : DEFAULT_ACCOUNTS) {
            boolean found = userWallets.stream()
                .anyMatch(w -> w.getAccountType().equalsIgnoreCase(defAcc.name));
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Inner class to represent default account configuration
     */
    public static class DefaultAccount {
        public final String name;
        public final String currency;
        public final String description;

        public DefaultAccount(String name, String currency, String description) {
            this.name = name;
            this.currency = currency;
            this.description = description;
        }
    }
}
