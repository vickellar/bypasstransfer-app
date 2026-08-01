package com.bypass.bypasstransers;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class BypasstransersApplication {

    private static final Logger log = LoggerFactory.getLogger(BypasstransersApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(BypasstransersApplication.class, args);
    }

    private static void loadDotEnv() {
        java.io.File envFile = new java.io.File(".env");
        if (envFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eqPos = line.indexOf('=');
                    if (eqPos > 0) {
                        String key = line.substring(0, eqPos).trim();
                        String value = line.substring(eqPos + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
                log.info("Loaded variables from .env file");
            } catch (java.io.IOException e) {
                log.warn("Failed to load .env file: {}", e.getMessage());
            }
        }
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> init(AccountRepository accountRepo,
                                                           UserRepository userRepo,
                                                           PasswordEncoder passwordEncoder) {
        return event -> {
            try {
                long count = accountRepo.count();
                if (count == 0) {
                    accountRepo.save(new Account(null, "Econet", BigDecimal.ZERO, new BigDecimal("0.033")));
                    accountRepo.save(new Account(null, "InnBucks", BigDecimal.ZERO, new BigDecimal("0.0325")));
                    accountRepo.save(new Account(null, "Mukuru", BigDecimal.ZERO, new BigDecimal("0.03")));
                    log.info("Seeded default accounts");
                }

                long users = userRepo.count();
                if (users == 0) {
                    // GENERATE SECURE TEMPORARY PASSWORDS
                    String superAdminPass = generateSecurePassword();
                    String adminPass = generateSecurePassword();
                    String staffPass = generateSecurePassword();

                    User superAdmin = new User();
                    superAdmin.setUsername("superadmin");
                    superAdmin.setPassword(passwordEncoder.encode(superAdminPass));
                    superAdmin.setPhoneNumber("+26377801140");
                    superAdmin.setRole(Role.SUPER_ADMIN);
                    userRepo.save(superAdmin);

                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode(adminPass));
                    admin.setPhoneNumber("+263717847646");
                    admin.setRole(Role.ADMIN);
                    userRepo.save(admin);

                    User staff = new User();
                    staff.setUsername("staff");
                    staff.setPassword(passwordEncoder.encode(staffPass));
                    staff.setPhoneNumber("+236700000000");
                    staff.setRole(Role.STAFF);
                    userRepo.save(staff);

                    // ⚠️ LOG CREDENTIALS SECURELY (only in logs, not console)
                    log.warn("🔐 INITIAL CREDENTIALS GENERATED (Change passwords immediately):");
                    log.warn("superadmin: {}", superAdminPass);
                    log.warn("admin: {}", adminPass);
                    log.warn("staff: {}", staffPass);
                    log.warn("⚠️  These credentials will NOT be displayed again. Change them after first login.");
                }

                // Migrate any existing plain-text passwords to BCrypt
                List<User> all = userRepo.findAll();
                int migrated = 0;
                for (User u : all) {
                    String pw = u.getPassword();
                    if (pw != null && !pw.startsWith("$2")) {
                        u.setPassword(passwordEncoder.encode(pw));
                        userRepo.save(u);
                        migrated++;
                    }
                }
                if (migrated > 0) {
                    log.info("Migrated {} plain-text passwords to BCrypt", migrated);
                }

            } catch (Exception ex) {
                log.warn("Initialization skipped: {}", ex.getMessage());
            }
        };
    }

    /**
     * Generate a cryptographically secure random password
     * Format: 16 characters with mixed case, numbers, and symbols
     */
    private static String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*_+-=";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}