package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial.password:${ADMIN_PASSWORD:admin123}}")
    private String adminInitialPassword;

    public DataLoader(AccountRepository accountRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // First, try to add the missing columns if they don't exist
            addMissingColumns();
            
            // Check if admin user exists, create if not
            // Use findAll and filter to handle case where duplicates might exist
            List<User> adminUsers = userRepository.findAll().stream()
                .filter(u -> "admin".equalsIgnoreCase(u.getUsername()))
                .toList();
            
            final User admin;
            if (adminUsers.isEmpty()) {
                User newAdmin = new User();
                newAdmin.setUsername("admin");
                // Password set from env var ADMIN_PASSWORD - change after first login
                newAdmin.setPassword(passwordEncoder.encode(adminInitialPassword));
                newAdmin.setRole(Role.SUPER_ADMIN);
                newAdmin.setPhoneNumber("+263000000000");
                newAdmin.setEmail("admin@bypasstransfers.com");
                userRepository.save(newAdmin);
                admin = newAdmin;
                System.out.println("=================================================");
                System.out.println("DEFAULT ADMIN CREATED - CHANGE PASSWORD IMMEDIATELY");
                System.out.println("Username: admin");
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

                // Do NOT reset password on startup - preserves admin-set passwords
                System.out.println("Admin user already exists. Startup check complete.");
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
        } catch (InvalidDataAccessResourceUsageException e) {
            // This exception occurs when the is_active column doesn't exist yet
            // This means the migrations haven't run yet, so we'll skip data loading for now
            System.out.println("Database schema not yet updated. Skipping data loading.");
            System.out.println("Please restart the application after the migrations complete.");
            return;
        }
    }

    /**
     * Add missing columns to the users table if they don't exist
     * This handles the database migration issue gracefully
     */
    private void addMissingColumns() {
        try {
            // Try to query a user to see if is_active column exists
            userRepository.findAll();
            System.out.println("Database schema appears to be up to date.");
        } catch (Exception e) {
            System.out.println("Database schema needs updating. Attempting to add missing columns...");
            
            // This is where we would normally run database migrations
            // For now, we'll let Hibernate handle it or inform the user
            System.out.println("Please ensure your database has the following columns in the users table:");
            System.out.println("- is_active (boolean, NOT NULL, default true)");
            System.out.println("- deleted_at (timestamp without time zone)");
            System.out.println("You can run the SQL migration manually or let Hibernate auto-create it.");
        }
    }
}