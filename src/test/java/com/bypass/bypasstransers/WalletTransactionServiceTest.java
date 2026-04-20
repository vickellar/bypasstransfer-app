package com.bypass.bypasstransers;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.service.WalletTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;

@SpringBootTest
@ActiveProfiles("dev")
public class WalletTransactionServiceTest {

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    public void setup() {
        SecurityService dummySecurityService = new SecurityService() {
            @Override
            public User getCurrentUser() {
                return userRepository.findByUsername("testuser").stream().findFirst().orElse(null);
            }
        };
        ReflectionTestUtils.setField(walletTransactionService, "securityService", dummySecurityService);
    }

    @Test
    public void testSend() {
        // Setup data
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("pass");
        user = userRepository.save(user);

        Account pct = new Account();
        pct.setName("TestAccount");
        pct.setTransferFee(new BigDecimal("0.05"));
        accountRepository.save(pct);

        Wallet wallet = new Wallet();
        wallet.setOwner(user);
        wallet.setAccountType("TestAccount");
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setCurrency(com.bypass.bypasstransers.enums.Currency.USD);
        walletRepository.save(wallet);

        // Attempt to send
        try {
            walletTransactionService.send("TestAccount", new BigDecimal("100.00"));
            System.out.println("SUCCESS! No exception thrown.");
        } catch (Exception e) {
            System.err.println("CAUGHT EXCEPTION IN TEST:");
            e.printStackTrace();
        }
    }
}
