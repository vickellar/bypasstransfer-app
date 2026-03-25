package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.model.Wallet;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconsiliationService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private DailyReconciliationRepository dailyRepo;

    @Transactional
    public DailyReconciliation reconcile(LocalDate date, double actualBalance) {

        double systemBalance = walletRepository.findAll()
                .stream()
                .mapToDouble(Wallet::getBalance)
                .sum();

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(date);
        r.setSystemBalance(systemBalance);
        r.setActualBalance(actualBalance);
        r.setDifference(actualBalance - systemBalance);
        r.setAccountName("TOTAL_SYSTEM");

        return dailyRepo.save(r);
    }

    @Transactional
    public DailyReconciliation reconcileWallet(Long walletId, double actualBalance) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(LocalDate.now());
        r.setSystemBalance(wallet.getBalance());
        r.setActualBalance(actualBalance);
        r.setDifference(actualBalance - wallet.getBalance());
        r.setWalletId(wallet.getId());
        r.setAccountName(wallet.getAccountType());

        return dailyRepo.save(r);
    }

}