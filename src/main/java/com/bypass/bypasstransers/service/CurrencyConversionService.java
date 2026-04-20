package com.bypass.bypasstransers.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class CurrencyConversionService {

    @Autowired
    private ExchangeRateService exchangeRateService;

    /**
     * Convert amount from one currency to another using ExchangeRateService.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) return BigDecimal.ZERO;
        return exchangeRateService.convert(amount, fromCurrency, toCurrency);
    }

    /**
     * Get exchange rate between two currencies using ExchangeRateService.
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        return exchangeRateService.getExchangeRate(fromCurrency, toCurrency);
    }

    /**
     * Update or create exchange rate - now handled via ExchangeRateService manual update.
     */
    public void updateExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, String source) {
        // We assume USD base for the consolidated service manual updates
        if ("USD".equals(fromCurrency)) {
            exchangeRateService.setManualRate(toCurrency, rate);
        } else {
            // If it's a non-USD pair, we just log it as the consolidated service focuses on USD base rates
        }
    }
}
