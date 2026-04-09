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

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class BypasstransersApplication {

    private static final Logger log =
            LoggerFactory.getLogger(BypasstransersApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(BypasstransersApplication.class, args);
    }

    private static void loadDotEnv() {
        // 1. Detect Render DATABASE_URL and convert to Spring properties
        String renderDbUrl = System.getenv("DATABASE_URL");
        if (renderDbUrl != null && !renderDbUrl.isEmpty() && renderDbUrl.startsWith("postgres://")) {
            try {
                log.info("Detected Render DATABASE_URL, configuring DataSource...");
                java.net.URI dbUri = new java.net.URI(renderDbUrl);
                String userInfo = dbUri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String user = userInfo.split(":")[0];
                    String password = userInfo.split(":")[1];
                    String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();
                    
                    if (System.getProperty("spring.datasource.url") == null)
                        System.setProperty("spring.datasource.url", jdbcUrl);
                    if (System.getProperty("spring.datasource.username") == null)
                        System.setProperty("spring.datasource.username", user);
                    if (System.getProperty("spring.datasource.password") == null)
                        System.setProperty("spring.datasource.password", password);
                }
            } catch (Exception e) {
                log.warn("Failed to parse Render DATABASE_URL: {}", e.getMessage());
            }
        }

        // 2. Load from .env file for local development
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
                    accountRepo.save(new Account(null, "Econet", 0.0, 0.033));
                    accountRepo.save(new Account(null, "InnBucks", 0.0, 0.02));
                    accountRepo.save(new Account(null, "Mukuru", 0.0, 0.015));
                    log.info("Seeded default accounts");
                }

                long users = userRepo.count();
                if (users == 0) {
                    // create a super admin
                    User superAdmin = new User();
                    superAdmin.setUsername("superadmin");
                    superAdmin.setPassword(passwordEncoder.encode("superpass"));
                    superAdmin.setPhoneNumber("+26377801140");
                    superAdmin.setRole(Role.SUPER_ADMIN);
                    userRepo.save(superAdmin);

                    // create an admin
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode("adminpass"));
                    admin.setPhoneNumber("+263717847646");
                    admin.setRole(Role.ADMIN);
                    userRepo.save(admin);

                    // create a staff user
                    User staff = new User();
                    staff.setUsername("staff");
                    staff.setPassword(passwordEncoder.encode("staffpass"));
                    staff.setPhoneNumber("+1000000002");
                    staff.setRole(Role.STAFF);
                    userRepo.save(staff);

                    log.info("Seeded default users: superadmin/admin/staff");
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

}