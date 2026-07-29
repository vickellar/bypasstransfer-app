package com.bypass.bypasstransers;

import com.bypass.bypasstransers.config.DatabaseEnvironmentConfigurer;
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

    private static final Logger log =
            LoggerFactory.getLogger(BypasstransersApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        DatabaseEnvironmentConfigurer.configure();
        SpringApplication.run(BypasstransersApplication.class, args);
    }

    private static void loadDotEnv() {
        // Load from .env file for local development
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
                        // Priority: Environment Variable > System Property > .env file
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

    /**
     * Seed default accounts AND default users AFTER application startup
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> init(AccountRepository accountRepo,
                                                            UserRepository userRepo,
                                                            PasswordEncoder passwordEncoder) {
        return event -> {
            try {
                long count = accountRepo.count();
                if (count == 0) {
                    accountRepo.save(new Account(null, "Econet", BigDecimal.ZERO, new BigDecimal("0.033")));
                    accountRepo.save(new Account(null, "InnBucks", BigDecimal.ZERO, new BigDecimal("0.02")));
                    accountRepo.save(new Account(null, "Mukuru", BigDecimal.ZERO, new BigDecimal("0.015")));
                    log.info("Seeded default accounts");
                }

                long users = userRepo.count();
                if (users == 0) {
                    // Read credentials from environment variables; generate secure random passwords if not set
                    String superAdminPass = getEnvOrDefault("SUPERADMIN_PASSWORD", null);
                    String adminPass = getEnvOrDefault("ADMIN_PASSWORD", null);
                    String staffPass = getEnvOrDefault("STAFF_PASSWORD", null);

                    boolean generated = false;
                    if (superAdminPass == null) { superAdminPass = generateSecurePassword(); generated = true; }
                    if (adminPass == null) { adminPass = generateSecurePassword(); generated = true; }
                    if (staffPass == null) { staffPass = generateSecurePassword(); generated = true; }

                    // create a super admin
                    User superAdmin = new User();
                    superAdmin.setUsername(getEnvOrDefault("SUPERADMIN_USERNAME", "superadmin"));
                    superAdmin.setPassword(passwordEncoder.encode(superAdminPass));
                    superAdmin.setPhoneNumber("+26377801140");
                    superAdmin.setRole(Role.SUPER_ADMIN);
                    userRepo.save(superAdmin);

                    // create an admin
                    User admin = new User();
                    admin.setUsername(getEnvOrDefault("ADMIN_USERNAME", "admin"));
                    admin.setPassword(passwordEncoder.encode(adminPass));
                    admin.setPhoneNumber("+263717847646");
                    admin.setRole(Role.ADMIN);
                    userRepo.save(admin);

                    // create a staff user
                    User staff = new User();
                    staff.setUsername(getEnvOrDefault("STAFF_USERNAME", "staff"));
                    staff.setPassword(passwordEncoder.encode(staffPass));
                    staff.setPhoneNumber("+1000000002");
                    staff.setRole(Role.STAFF);
                    userRepo.save(staff);

                    log.info("Seeded default users: superadmin/admin/staff");
                    if (generated) {
                        log.warn("========================================");
                        log.warn("GENERATED TEMPORARY PASSWORDS (change immediately!):");
                        log.warn("  superadmin password: {}", superAdminPass);
                        log.warn("  admin password:      {}", adminPass);
                        log.warn("  staff password:      {}", staffPass);
                        log.warn("Set SUPERADMIN_PASSWORD, ADMIN_PASSWORD, STAFF_PASSWORD env vars to avoid this.");
                        log.warn("========================================");
                    }
                }

                // Migrate any existing plain-text passwords to BCrypt
                List<User> all = userRepo.findAll();
                int migrated = 0;
                for (User u : all) {
                    String pw = u.getPassword();
                    if (pw != null && !pw.startsWith("$2")) {
                        // treat existing value as raw password and encode it
                        u.setPassword(passwordEncoder.encode(pw));
                        userRepo.save(u);
                        migrated++;
                    }
                }
                if (migrated > 0) log.info("Migrated {} plain-text passwords to BCrypt", migrated);

            } catch (Exception ex) {
                log.warn("Initialization skipped: {}", ex.getMessage());
            }
        };
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val;
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) return prop;
        return defaultValue;
    }

    private static String generateSecurePassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "@$!%*?&";
        String all = upper + lower + digits + special;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        // Guarantee at least one of each category
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        for (int i = 4; i < 16; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        // Shuffle to avoid predictable positions
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

}