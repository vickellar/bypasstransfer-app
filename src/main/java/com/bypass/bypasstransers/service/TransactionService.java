package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private AlertService alertService;

    /* 1️⃣ RECEIVE MONEY */
    public void receive(String accountName, double amount) {
        Account acc = accountRepo.findByName(accountName);
        acc.setBalance(acc.getBalance() + amount);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.INCOME);
        tx.setAmount(amount);
        tx.setFee(0);
        tx.setToAccount(accountName);
        tx.setDate(LocalDateTime.now());

        accountRepo.save(acc);
        txRepo.save(tx);

        checkLowBalance(acc);
    }

    /* 2️⃣ SEND MONEY (EXPENSE) */
    public void send(String accountName, double amount) {
        Account acc = accountRepo.findByName(accountName);

        double fee = ChargeCalculator.calculateFee(acc, amount);
        double total = amount + fee;

        if (acc.getBalance() < total) {
            throw new RuntimeException("Insufficient balance");
        }

        acc.setBalance(acc.getBalance() - total);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setFromAccount(accountName);
        tx.setDate(LocalDateTime.now());

        accountRepo.save(acc);
        txRepo.save(tx);

        checkLowBalance(acc);
    }

    public List<Transaction> findAll() {
        return txRepo.findAll();
    }

    public Transaction save(Transaction tx) {
        tx.setTransactionDate(LocalDateTime.now());
        tx.setSyncStatus("SYNC_PENDING");
        return txRepo.save(tx);
    }

    /* 3️⃣ TRANSFER BETWEEN ACCOUNTS */
    public void transfer(String fromName, String toName, double amount) {
        Account from = accountRepo.findByName(fromName);
        Account to = accountRepo.findByName(toName);

        double fee = ChargeCalculator.calculateFee(from, amount);
        double total = amount + fee;

        if (from.getBalance() < total) {
            throw new RuntimeException("Insufficient balance");
        }

        from.setBalance(from.getBalance() - total);
        to.setBalance(to.getBalance() + amount);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setFromAccount(fromName);
        tx.setToAccount(toName);
        tx.setDate(LocalDateTime.now());

        accountRepo.save(from);
        accountRepo.save(to);
        txRepo.save(tx);

        checkLowBalance(from);
        checkLowBalance(to);
    }

    /* 4️⃣ PROFIT & FEES */
    public double totalFees() {
        return txRepo.findAll()
                .stream()
                .mapToDouble(Transaction::getFee)
                .sum();
    }

    private void checkLowBalance(Account account) {
        if (account.getBalance() < account.getLowBalanceThreshold()
                && !account.isLowBalanceAlertSent()) {

            alertService.notifyLowBalance(account);
            // persist the flag change
            accountRepo.save(account);
        }
    }

}