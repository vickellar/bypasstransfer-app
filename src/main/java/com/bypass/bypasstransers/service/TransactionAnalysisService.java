/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionAnalysisService {

    @Autowired
    private TransactionRepository repo;

    public double totalVolume() {
        return repo.findAll()
                   .stream()
                   .mapToDouble(Transaction::getAmount)
                   .sum();
    }

    public double totalFees() {
        return repo.findAll()
                   .stream()
                   .mapToDouble(Transaction::getFee)
                   .sum();
    }

    public Map<String, Double> volumePerAccount() {
        Map<String, Double> map = new HashMap<>();

        for (Object[] row : repo.volumeByAccount()) {
            map.put((String) row[0], (Double) row[1]);
        }
        return map;
    }
    public List<Transaction> suspiciousTransactions(double threshold) {
    return repo.findAll()
        .stream()
        .filter(t -> t.getAmount() > threshold)
        .toList();
}

}
