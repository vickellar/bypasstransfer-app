package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bypass.bypasstransers.util.ChargeCalculator;

@Service
public class ExchangeRateService {

    // Simulated exchange rates with USD as base currency
    private final Map<String, BigDecimal> exchangeRates = new HashMap<>();

    @Autowired
    private TransactionRepository transactionRepository;

    public ExchangeRateService() {
        // Initialize with realistic exchange rates (based on USD as base)
        // These are rates where 1 USD = X units of other currency
        exchangeRates.put("USD", BigDecimal.ONE);           // US Dollar (base)
        exchangeRates.put("ZWL", new BigDecimal("3.75"));  // Zimbabwean Dollar (1 USD = 3.75 ZWL)
        exchangeRates.put("ZAR", new BigDecimal("18.45")); // South African Rand (1 USD = 18.45 ZAR)
        exchangeRates.put("GBP", new BigDecimal("0.79"));  // British Pound (1 USD = 0.79 GBP)
        exchangeRates.put("EUR", new BigDecimal("0.92"));  // Euro (1 USD = 0.92 EUR)
        exchangeRates.put("RUB", new BigDecimal("92.50")); // Russian Ruble (1 USD = 92.50 RUB)
        exchangeRates.put("KES", new BigDecimal("150.00")); // Kenyan Shilling (1 USD = 150 KES)
        exchangeRates.put("UGX", new BigDecimal("3700.00")); // Ugandan Shilling (1 USD = 3700 UGX)
        exchangeRates.put("TZS", new BigDecimal("2300.00")); // Tanzanian Shilling (1 USD = 2300 TZS)
        exchangeRates.put("MWK", new BigDecimal("1700.00")); // Malawian Kwacha (1 USD = 1700 MWK)
        exchangeRates.put("ZMW", new BigDecimal("25.00")); // Zambian Kwacha (1 USD = 25 ZMW)
        exchangeRates.put("BWP", new BigDecimal("12.50")); // Botswana Pula (1 USD = 12.50 BWP)
    }

    /**
     * Get exchange rate from one currency to another
     * This calculates how many units of 'toCurrency' you get for 1 unit of 'fromCurrency'
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        BigDecimal fromRate = exchangeRates.get(fromCurrency);
        BigDecimal toRate = exchangeRates.get(toCurrency);

        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + fromCurrency + " or " + toCurrency);
        }

        // To convert from one currency to another using USD as base:
        // FromCurrency -> USD -> ToCurrency
        // First, find how much USD we get for 1 unit of fromCurrency
        // 1 fromCurrency = (1 / fromRate) USD
        // Then convert USD to toCurrency
        // (1 / fromRate) USD = (1 / fromRate) * toRate toCurrency
        // So 1 fromCurrency = (toRate / fromRate) toCurrency
        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }

    /**
     * Convert amount from one currency to another
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get all supported currencies
     */
    public Map<String, BigDecimal> getAllRates() {
        return new HashMap<>(exchangeRates);
    }

    /**
     * Calculate amount after applying transaction fee
     */
    public BigDecimal calculateNetAmount(BigDecimal grossAmount, BigDecimal feePercentage) {
        BigDecimal feeDecimal = feePercentage.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal netMultiplier = BigDecimal.ONE.subtract(feeDecimal);
        return grossAmount.multiply(netMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate transaction fee amount
     */
    public BigDecimal calculateFeeAmount(BigDecimal grossAmount, BigDecimal feePercentage) {
        BigDecimal feeDecimal = feePercentage.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        return grossAmount.multiply(feeDecimal).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get the USD equivalent rate for a given currency
     * Returns how many units of the currency equal 1 USD
     */
    public BigDecimal getUsdRate(String currency) {
        return exchangeRates.getOrDefault(currency, BigDecimal.ZERO);
    }
    
    /**
     * Get rate to convert FROM a currency TO USD
     * This is the inverse of getUsdRate - it returns how many USD you get for 1 unit of the currency
     */
    public BigDecimal getRateToUsd(String currency) {
        BigDecimal usdRate = exchangeRates.get(currency);
        if (usdRate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        if (BigDecimal.ONE.compareTo(usdRate) == 0) {
            // If currency is USD, return 1 (1 USD = 1 USD)
            return BigDecimal.ONE;
        }
        // Return the inverse: 1 / usdRate
        return BigDecimal.ONE.divide(usdRate, 6, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate total profit for the business (sum of all fees collected)
     */
    public BigDecimal calculateTotalProfit() {
        List<Transaction> allTransactions = transactionRepository.findAll();
        BigDecimal totalProfit = BigDecimal.ZERO;
        
        for (Transaction transaction : allTransactions) {
            if (transaction.getType() != null && transaction.getType() != TransactionType.INCOME) {
                BigDecimal profit = new BigDecimal(transaction.getAmount()).multiply(
                        BigDecimal.valueOf(ChargeCalculator.BASE_PROFIT_DEFAULT));
                totalProfit = totalProfit.add(profit);
            }
        }
        
        return totalProfit.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate profit for a specific period
     */
    public BigDecimal calculateProfitForPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        BigDecimal totalProfit = BigDecimal.ZERO;
        
        for (Transaction transaction : allTransactions) {
            java.time.LocalDate transactionDate = transaction.getDate().toLocalDate();
            if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                if (transaction.getType() != null && transaction.getType() != TransactionType.INCOME) {
                    BigDecimal profit = new BigDecimal(transaction.getAmount()).multiply(
                            BigDecimal.valueOf(ChargeCalculator.BASE_PROFIT_DEFAULT));
                    totalProfit = totalProfit.add(profit);
                }
            }
        }
        
        return totalProfit.setScale(2, RoundingMode.HALF_UP);
    }
}