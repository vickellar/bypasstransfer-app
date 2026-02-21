package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.enums.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
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
        // create default user if absent
        Optional<User> maybe = userRepository.findAll().stream().findFirst();
        User user;
        if (maybe.isPresent()) {
            user = maybe.get();
        } else {
            user = new User();
            user.setUsername("vickellar");
            // store a BCrypt-hashed password so authentication works
            user.setPassword(passwordEncoder.encode("NjisweVic~2030"));
            // set a default role to allow role-based access checks (use STAFF by default)
            user.setRole(Role.STAFF);
            user.setPhoneNumber("+263778411140");
            userRepository.save(user);
        }

        // Seed accounts if not present
        if (accountRepository.findByName("Econet") == null) {
            Account econet = new Account();
            econet.setName("Econet");
            econet.setBalance(1000.0);
            econet.setTransferFee(0.033); // 3.3%
            econet.setOwner(user);
            accountRepository.save(econet);
        }

        if (accountRepository.findByName("InnBucks") == null) {
            Account innbucks = new Account();
            innbucks.setName("InnBucks");
            innbucks.setBalance(500.0);
            innbucks.setTransferFee(0.02); // 2%
            innbucks.setOwner(user);
            accountRepository.save(innbucks);
        }

        if (accountRepository.findByName("Mukuru") == null) {
            Account mukuru = new Account();
            mukuru.setName("Mukuru");
            mukuru.setBalance(300.0);
            mukuru.setTransferFee(0.015); // 1.5% placeholder
            mukuru.setOwner(user);
            accountRepository.save(mukuru);
        }
    }
}