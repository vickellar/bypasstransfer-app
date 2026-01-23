
package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.ExchangeRate;
import com.bypass.bypasstransers.repository.ExchangeRateRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    @Autowired
    private ExchangeRateRepository repo;

    public double convert(String from, String to, double amount) {
        ExchangeRate rate =
            repo.findTopByBaseCurrencyAndTargetCurrencyOrderByFetchedAtDesc(from, to);

        if (rate == null) {
            throw new RuntimeException("No exchange rate configured");
        }

        return amount * rate.getRate();
    }

    public void saveRate(String from, String to, double rate, String source) {
        ExchangeRate r = new ExchangeRate();
        r.setBaseCurrency(from);
        r.setTargetCurrency(to);
        r.setRate(rate);
        r.setFetchedAt(LocalDateTime.now());
        r.setSource(source);
        repo.save(r);
    }
}
