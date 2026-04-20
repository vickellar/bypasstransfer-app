package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionAnalysisService {

    @Autowired
    private TransactionRepository repo;

    public BigDecimal totalVolume() {
        return repo.findAll()
                   .stream()
                   .map(Transaction::getAmount)
                   .reduce(BigDecimal.ZERO, BigDecimal::add)
                   .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalFees() {
        return repo.findAll()
                   .stream()
                   .map(Transaction::getFee)
                   .reduce(BigDecimal.ZERO, BigDecimal::add)
                   .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> volumePerAccount() {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : repo.volumeByAccount()) {
            if (row == null || row.length < 2) continue;
            String name = (String) row[0];
            Object valObj = row[1];
            BigDecimal value = BigDecimal.ZERO;
            if (valObj instanceof BigDecimal) {
                value = (BigDecimal) valObj;
            } else if (valObj instanceof Number) {
                value = new BigDecimal(valObj.toString());
            }
            map.put(name, value.setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    public List<Transaction> suspiciousTransactions(BigDecimal threshold) {
        return repo.findAll()
            .stream()
            .filter(t -> t.getAmount().compareTo(threshold) > 0)
            .toList();
    }
}
