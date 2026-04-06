package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.enums.Role;
import com.bypass.bypasstransers.service.UserProvisioningService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProvisioningService userProvisioningService;

    @Value("${admin.initial.password:admin123}")
    private String adminInitialPassword;

    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder,
            UserProvisioningService userProvisioningService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create initial admin if not present
        Optional<User> adminOpt = userRepository.findAll().stream()
                .filter(u -> "admin".equalsIgnoreCase(u.getUsername()))
                .findFirst();

        if (adminOpt.isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminInitialPassword));
            admin.setRole(Role.SUPER_ADMIN);
            admin.setPhoneNumber("+263778411140");
            admin.setIsActive(true);
            userRepository.save(admin);
            
            // Create default wallets for admin
            userProvisioningService.createDefaultWalletsForUser(admin);
            
            System.out.println("Default admin created: 'admin' with initial password.");
        }
    }
}