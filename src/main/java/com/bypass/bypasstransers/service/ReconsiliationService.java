package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconsiliationService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DailyReconciliationRepository dailyRepo;

    @Transactional
    public DailyReconciliation reconcile(LocalDate date, double actualBalance) {

        double systemBalance = accountRepository.findAll()
                .stream()
                .mapToDouble(Account::getBalance)
                .sum();

        DailyReconciliation r = new DailyReconciliation();
        r.setDate(date);
        r.setSystemBalance(systemBalance);
        r.setActualBalance(actualBalance);
        r.setDifference(actualBalance - systemBalance);

        return dailyRepo.save(r);
    }

}