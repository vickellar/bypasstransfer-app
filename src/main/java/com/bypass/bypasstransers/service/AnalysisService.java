package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    @Autowired
    private TransactionRepository repo;

    public Map<LocalDate, Double> dailyVolume() {
        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (Object[] r : repo.dailyVolume()) {
            if (r == null || r.length < 2) continue;
            Object dateObj = r[0];
            LocalDate key;
            if (dateObj instanceof LocalDate) {
                key = (LocalDate) dateObj;
            } else if (dateObj instanceof java.time.LocalDateTime) {
                key = ((java.time.LocalDateTime) dateObj).toLocalDate();
            } else if (dateObj instanceof java.sql.Date) {
                key = ((java.sql.Date) dateObj).toLocalDate();
            } else {
                // fallback parse
                key = LocalDate.parse(dateObj.toString());
            }

            Object valObj = r[1];
            double value = 0.0;
            if (valObj instanceof Number) {
                value = ((Number) valObj).doubleValue();
            } else if (valObj != null) {
                try {
                    value = Double.parseDouble(valObj.toString());
                } catch (Exception e) {
                    value = 0.0;
                }
            }

            map.put(key, value);
        }
        return map;
    }

    public Map<String, Double> feesByAccount() {
        Map<String, Double> map = new HashMap<>();
        for (Object[] r : repo.feesByAccount()) {
            if (r == null || r.length < 2) continue;
            String acct = r[0] != null ? r[0].toString() : "";
            Object valObj = r[1];
            double value = 0.0;
            if (valObj instanceof Number) {
                value = ((Number) valObj).doubleValue();
            } else if (valObj != null) {
                try {
                    value = Double.parseDouble(valObj.toString());
                } catch (Exception e) {
                    value = 0.0;
                }
            }
            map.put(acct, value);
        }
        return map;
    }

    public double totalProfit() {
        return repo.findAll()
                .stream()
                .mapToDouble(Transaction::getFee)
                .sum();
    }
}