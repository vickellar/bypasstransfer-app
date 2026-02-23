package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.enums.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(AccountRepository accountRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user exists, create if not
        // Use findAll and filter to handle case where duplicates might exist
        List<User> adminUsers = userRepository.findAll().stream()
            .filter(u -> "admin".equalsIgnoreCase(u.getUsername()))
            .toList();
        
        final User admin;
        if (adminUsers.isEmpty()) {
            User newAdmin = new User();
            newAdmin.setUsername("admin");
            // Default admin password - should be changed on first login
            newAdmin.setPassword(passwordEncoder.encode("admin123"));
            newAdmin.setRole(Role.SUPER_ADMIN);
            newAdmin.setPhoneNumber("+263000000000");
            newAdmin.setEmail("admin@bypasstransfers.com");
            userRepository.save(newAdmin);
            admin = newAdmin;
            System.out.println("=================================================");
            System.out.println("DEFAULT ADMIN CREATED:");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("Role: SUPER_ADMIN");
            System.out.println("=================================================");
        } else {
            admin = adminUsers.get(0); // Use the first admin found
            
            // DELETE duplicate admin users (keep only the first one)
            if (adminUsers.size() > 1) {
                System.out.println("WARNING: Multiple admin users found. Deleting duplicates...");
                for (int i = 1; i < adminUsers.size(); i++) {
                    User duplicate = adminUsers.get(i);
                    System.out.println("Deleting duplicate admin with ID: " + duplicate.getId());
                    userRepository.delete(duplicate);
                }
                System.out.println("Duplicate admins deleted. Only one admin remains.");
            }
            
            // Reset password to ensure admin can always log in
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.SUPER_ADMIN); // Ensure role is SUPER_ADMIN
            userRepository.save(admin);
            
            System.out.println("=================================================");
            System.out.println("ADMIN USER EXISTS - PASSWORD RESET:");
            System.out.println("Username: " + admin.getUsername());
            System.out.println("Password: admin123");
            System.out.println("Role: " + admin.getRole());
            System.out.println("=================================================");
        }

        // Seed accounts for admin if not present
        // Use a direct query to find accounts by owner ID to avoid NonUniqueResultException
        List<Account> adminAccounts = accountRepository.findByOwnerId(admin.getId());
        
        boolean hasEconet = adminAccounts.stream().anyMatch(a -> "Econet".equals(a.getName()));
        boolean hasInnBucks = adminAccounts.stream().anyMatch(a -> "InnBucks".equals(a.getName()));
        boolean hasMukuru = adminAccounts.stream().anyMatch(a -> "Mukuru".equals(a.getName()));
        
        if (!hasEconet) {
            Account econet = new Account();
            econet.setName("Econet");
            econet.setBalance(1000.0);
            econet.setTransferFee(0.033); // 3.3%
            econet.setOwner(admin);
            accountRepository.save(econet);
        }

        if (!hasInnBucks) {
            Account innbucks = new Account();
            innbucks.setName("InnBucks");
            innbucks.setBalance(500.0);
            innbucks.setTransferFee(0.02); // 2%
            innbucks.setOwner(admin);
            accountRepository.save(innbucks);
        }

        if (!hasMukuru) {
            Account mukuru = new Account();
            mukuru.setName("Mukuru");
            mukuru.setBalance(300.0);
            mukuru.setTransferFee(0.015); // 1.5% placeholder
            mukuru.setOwner(admin);
            accountRepository.save(mukuru);
        }
    }
}