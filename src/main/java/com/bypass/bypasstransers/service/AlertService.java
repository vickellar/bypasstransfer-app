package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Handles low balance alerts and sends SMS notifications to account owners.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SmsService smsService;

    public List<Account> lowBalance(BigDecimal threshold) {
        try {
            return accountRepository.findAll()
                    .stream()
                    .filter(a -> a.getBalance() != null && a.getBalance().compareTo(threshold) < 0)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch low balance accounts", e);
            return List.of();
        }
    }

    /** Returns accounts whose balance is below their own lowBalanceThreshold. */
    public List<Account> lowBalanceAccounts() {
        try {
            return accountRepository.findAll()
                    .stream()
                    .filter(a -> a.getBalance() != null && a.getLowBalanceThreshold() != null 
                             && a.getBalance().compareTo(a.getLowBalanceThreshold()) < 0)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch low balance accounts", e);
            return List.of();
        }
    }

    public void notifyLowBalance(Account account) {
        String message = "⚠ LOW BALANCE ALERT: " + account.getName()
                + " - Balance: " + account.getBalance()
                + " (threshold: " + account.getLowBalanceThreshold() + ")";

        log.warn(message);

        if (account.getOwner() != null) {
            try {
                smsService.sendAlert(account.getOwner(), message);
            } catch (Exception e) {
                log.warn("Failed to send low balance SMS to {}: {}", account.getOwner().getUsername(), e.getMessage());
            }
        }

        String username = account.getOwner() != null ? account.getOwner().getUsername() : "unknown";
        try {
            auditService.log(username, "Low balance alert sent for " + account.getName());
        } catch (Exception e) {
            log.warn("Failed to audit low balance alert: {}", e.getMessage());
        }

        account.setLowBalanceAlertSent(true);
        try {
            accountRepository.save(account);
        } catch (Exception e) {
            log.error("Failed to persist low balance alert flag", e);
        }
    }
}