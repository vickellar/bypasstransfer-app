package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bypass.bypasstransers.model.ExchangeRate;
import com.bypass.bypasstransers.repository.ExchangeRateRepository;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Service for managing currency exchange rates and providing conversion logic.
 */
@Service
public class ExchangeRateService {

    private final Map<String, BigDecimal> exchangeRates = new HashMap<>();

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Value("${exchange.rate.api.url:}")
    private String apiUrl;

    private boolean initialFetchDone = false;

    public ExchangeRateService() {
        // Will initialize in @PostConstruct
    }

    @PostConstruct
    public void initRates() {
        // 1. Initialize hardcoded defaults as a base fallback
        exchangeRates.put("USD", BigDecimal.ONE); 
        exchangeRates.put("ZWL", new BigDecimal("3750.00")); 
        exchangeRates.put("ZAR", new BigDecimal("18.45")); 
        exchangeRates.put("GBP", new BigDecimal("0.79")); 
        exchangeRates.put("EUR", new BigDecimal("0.92")); 
        exchangeRates.put("RUB", new BigDecimal("92.50")); 
        exchangeRates.put("KES", new BigDecimal("150.00")); 
        exchangeRates.put("UGX", new BigDecimal("3700.00")); 
        exchangeRates.put("TZS", new BigDecimal("2300.00")); 
        exchangeRates.put("MWK", new BigDecimal("1700.00")); 
        exchangeRates.put("ZMW", new BigDecimal("25.00")); 
        exchangeRates.put("BWP", new BigDecimal("12.50"));

        // 2. Override with values from database if present
        try {
            List<ExchangeRate> dbRates = exchangeRateRepository.findAll();
            for (ExchangeRate rate : dbRates) {
                if ("USD".equals(rate.getFromCurrency())) {
                    exchangeRates.put(rate.getToCurrency(), rate.getRate());
                }
            }
            if (!dbRates.isEmpty()) {
                System.out.println("[ExchangeRateService] Loaded " + dbRates.size() + " rates from database.");
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRateService] Failed to load from database: " + e.getMessage());
        }

        // 3. Trigger live fetch if API URL is available
        fetchLiveRates();
    }

    public synchronized void fetchLiveRates() {
        if (apiUrl == null || apiUrl.isBlank()) return;

        // Ensure URL ends with 'USD' for the v6 API as per convention in .env note 
        String fetchUrl = apiUrl;
        if (fetchUrl.endsWith("/latest/")) {
            fetchUrl += "USD";
        }

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                fetchUrl, 
                HttpMethod.GET, 
                null, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> response = responseEntity.getBody();
            if (response != null && response.containsKey("conversion_rates")) {
                Object ratesObj = response.get("conversion_rates");
                if (ratesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ratesMap = (Map<String, Object>) ratesObj;
                    for (Map.Entry<String, Object> entry : ratesMap.entrySet()) {
                        Object val = entry.getValue();
                        if (val instanceof Number) {
                            BigDecimal rateVal = new BigDecimal(val.toString());
                            String currency = entry.getKey();
                            exchangeRates.put(currency, rateVal);
                            
                            // Persist to database
                            updateDbRate("USD", currency, rateVal, "API");
                        }
                    }
                    System.out.println("[ExchangeRateService] Live rates updated from API.");
                    initialFetchDone = true;
                }
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRateService] API fetch failed: " + e.getMessage());
        }
    }

    private void updateDbRate(String from, String to, BigDecimal rate, String source) {
        try {
            java.util.Optional<ExchangeRate> existing = exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to);
            ExchangeRate entity;
            if (existing.isPresent()) {
                entity = existing.get();
            } else {
                entity = new ExchangeRate();
                entity.setFromCurrency(from);
                entity.setToCurrency(to);
            }
            entity.setRate(rate);
            entity.setSource(source);
            entity.setLastUpdated(java.time.LocalDateTime.now());
            exchangeRateRepository.save(entity);
        } catch (Exception e) {
            // Log quietly to avoid spamming
        }
    }

    public void setManualRate(String toCurrency, BigDecimal rate) {
        exchangeRates.put(toCurrency, rate);
        updateDbRate("USD", toCurrency, rate, "MANUAL");
    }

    /**
     * Get exchange rate from one currency to another
     * This calculates how many units of 'toCurrency' you get for 1 unit of
     * 'fromCurrency'
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

        // 1 fromCurrency = (toRate / fromRate) toCurrency
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
     */
    public BigDecimal getRateToUsd(String currency) {
        BigDecimal usdRate = exchangeRates.get(currency);
        if (usdRate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        if (BigDecimal.ONE.compareTo(usdRate) == 0) {
            return BigDecimal.ONE;
        }
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
                BigDecimal profit = transaction.getAmount().multiply(ChargeCalculator.BASE_PROFIT_DEFAULT);
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
                    BigDecimal profit = transaction.getAmount().multiply(ChargeCalculator.BASE_PROFIT_DEFAULT);
                    totalProfit = totalProfit.add(profit);
                }
            }
        }

        return totalProfit.setScale(2, RoundingMode.HALF_UP);
    }
}