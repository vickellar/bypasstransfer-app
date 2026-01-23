package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.repository.AccountRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Vickeller.01
 */
@Service
public class AlertService {

    @Autowired
    private AccountRepository accountRepository;

    public List<Account> lowBalance(double threshold) {
        try {
            return accountRepository.findAll()
                    .stream()
                    .filter(a -> a.getBalance() < threshold)
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Autowired
    private AuditService auditService;

    public void notifyLowBalance(Account account) {

        // For now: LOG + CONSOLE (SMS/Email later)
        System.out.println(
                "⚠ LOW BALANCE ALERT: " + account.getName()
                + " Balance = " + account.getBalance()
        );

        String username = "unknown";
        if (account.getOwner() != null) {
            username = account.getOwner().getUsername();
        }

        auditService.log(
                username,
                "Low balance alert sent for " + account.getName()
        );

        account.setLowBalanceAlertSent(true);
        try {
            accountRepository.save(account);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}