package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.ExchangeRate;
import com.bypass.bypasstransers.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class CurrencyConversionService {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * Convert amount from one currency to another using BigDecimal for precision.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) return BigDecimal.ZERO;
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // Try direct conversion
        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        
        if (rateOpt.isPresent()) {
            return amount.multiply(rateOpt.get().getRate()).setScale(4, RoundingMode.HALF_UP);
        }

        // Try reverse conversion (if to->from exists)
        Optional<ExchangeRate> reverseRateOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(toCurrency, fromCurrency);
        if (reverseRateOpt.isPresent()) {
            return amount.divide(reverseRateOpt.get().getRate(), 4, RoundingMode.HALF_UP);
        }

        // Try triangular arbitrage via USD
        if (!"USD".equals(fromCurrency) && !"USD".equals(toCurrency)) {
            Optional<ExchangeRate> fromToUsd = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, "USD");
            Optional<ExchangeRate> usdToTo = exchangeRateRepository.findByFromCurrencyAndToCurrency("USD", toCurrency);
            
            if (fromToUsd.isPresent() && usdToTo.isPresent()) {
                BigDecimal amountInUsd = amount.multiply(fromToUsd.get().getRate());
                return amountInUsd.multiply(usdToTo.get().getRate()).setScale(4, RoundingMode.HALF_UP);
            }
        }

        throw new RuntimeException(
            String.format("No exchange rate found for %s to %s", fromCurrency, toCurrency));
    }

    /**
     * Get exchange rate between two currencies
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        
        if (rateOpt.isPresent()) {
            return rateOpt.get().getRate();
        }

        // Try reverse
        Optional<ExchangeRate> reverseRateOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(toCurrency, fromCurrency);
        if (reverseRateOpt.isPresent()) {
            return BigDecimal.ONE.divide(reverseRateOpt.get().getRate(), 10, RoundingMode.HALF_UP);
        }

        throw new RuntimeException(
            String.format("No exchange rate found for %s to %s", fromCurrency, toCurrency));
    }

    /**
     * Update or create exchange rate
     */
    @Transactional
    public void updateExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, String source) {
        Optional<ExchangeRate> existingOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        
        if (existingOpt.isPresent()) {
            ExchangeRate rateEntity = existingOpt.get();
            rateEntity.setRate(rate);
            rateEntity.setSource(source);
            rateEntity.setLastUpdated(java.time.LocalDateTime.now());
            exchangeRateRepository.save(rateEntity);
        } else {
            ExchangeRate rateEntity = new ExchangeRate();
            rateEntity.setFromCurrency(fromCurrency);
            rateEntity.setToCurrency(toCurrency);
            rateEntity.setRate(rate);
            rateEntity.setSource(source);
            exchangeRateRepository.save(rateEntity);
        }
    }
}
