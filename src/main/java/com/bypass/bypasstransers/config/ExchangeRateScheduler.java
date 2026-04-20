package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.service.CurrencyConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Scheduled task to fetch exchange rates from external API.
 */
@Component
public class ExchangeRateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Autowired
    private WebClient webClient;

    @Value("${exchange.rate.api.key:demo}")
    private String apiKey;

    @Value("${exchange.rate.api.url:https://api.exchangerate-api.com/v4/latest/}")
    private String apiUrl;

    @Scheduled(cron = "${exchange.rate.update.cron:0 0 9 * * *}")
    public void updateExchangeRates() {
        logger.info("Starting scheduled exchange rate update...");
        try {
            String[] baseCurrencies = {"USD", "ZAR", "RUB", "EUR", "GBP"};
            for (String baseCurrency : baseCurrencies) {
                fetchAndUpdateRates(baseCurrency);
            }
            logger.info("Exchange rate update completed successfully");
        } catch (Exception e) {
            logger.error("Failed to update exchange rates: {}", e.getMessage());
        }
    }

    private void fetchAndUpdateRates(String baseCurrency) {
        try {
            String url = String.format("%s%s?key=%s", apiUrl, baseCurrency, apiKey);
            if ("demo".equals(apiKey)) {
                url = String.format("%s%s", apiUrl, baseCurrency);
            }

            // Null check to satisfy static analysis
            Objects.requireNonNull(url, "Generated exchange rate API URL cannot be null");

            var response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response != null) {
                org.json.JSONObject jsonObj = new org.json.JSONObject(response);
                if (jsonObj.has("rates")) {
                    org.json.JSONObject rates = jsonObj.getJSONObject("rates");
                    for (String targetCurrency : rates.keySet()) {
                        Object rateVal = rates.get(targetCurrency);
                        if (rateVal == null) continue;
                        
                        BigDecimal rate = new BigDecimal(rateVal.toString());
                        currencyConversionService.updateExchangeRate(baseCurrency, targetCurrency, rate, "API");
                        
                        if (rate.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal reverseRate = BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP);
                            currencyConversionService.updateExchangeRate(targetCurrency, baseCurrency, reverseRate, "API_DERIVED");
                        }
                    }
                    logger.info("Updated rates for base currency: {}", baseCurrency);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch rates for {}: {}", baseCurrency, e.getMessage());
        }
    }

    public void manualUpdate() {
        logger.info("Manual exchange rate update triggered");
        updateExchangeRates();
    }
}
