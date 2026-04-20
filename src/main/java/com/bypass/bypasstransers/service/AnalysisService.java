package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.util.ChargeCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public Map<LocalDate, BigDecimal> dailyVolume() {
        Map<LocalDate, BigDecimal> map = new LinkedHashMap<>();
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
                key = LocalDate.parse(dateObj.toString());
            }

            Object valObj = r[1];
            BigDecimal value = BigDecimal.ZERO;
            if (valObj instanceof BigDecimal) {
                value = (BigDecimal) valObj;
            } else if (valObj instanceof Number) {
                value = new BigDecimal(valObj.toString());
            } else if (valObj != null) {
                try {
                    value = new BigDecimal(valObj.toString());
                } catch (Exception e) {
                    value = BigDecimal.ZERO;
                }
            }

            map.put(key, value.setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    public Map<String, BigDecimal> feesByAccount() {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] r : repo.feesByAccount()) {
            if (r == null || r.length < 2) continue;
            String acct = r[0] != null ? r[0].toString() : "";
            Object valObj = r[1];
            BigDecimal value = BigDecimal.ZERO;
            if (valObj instanceof BigDecimal) {
                value = (BigDecimal) valObj;
            } else if (valObj instanceof Number) {
                value = new BigDecimal(valObj.toString());
            } else if (valObj != null) {
                try {
                    value = new BigDecimal(valObj.toString());
                } catch (Exception e) {
                    value = BigDecimal.ZERO;
                }
            }
            map.put(acct, value.setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    public BigDecimal totalProfit() {
        return repo.findAll()
                .stream()
                .filter(t -> t.getType() != null && t.getType() != TransactionType.INCOME)
                .map(t -> t.getAmount().multiply(ChargeCalculator.BASE_PROFIT_DEFAULT))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}