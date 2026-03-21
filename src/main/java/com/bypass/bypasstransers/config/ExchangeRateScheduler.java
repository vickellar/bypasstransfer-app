package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.service.CurrencyConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    /**
     * Scheduled task to update exchange rates daily at 9 AM UTC
     */
    @Scheduled(cron = "${exchange.rate.update.cron:0 0 9 * * *}")
    public void updateExchangeRates() {
        logger.info("Starting scheduled exchange rate update...");
        
        try {
            // Update rates for all supported currencies
            String[] baseCurrencies = {"USD", "ZAR", "RUB", "EUR", "GBP"};
            
            for (String baseCurrency : baseCurrencies) {
                fetchAndUpdateRates(baseCurrency);
            }
            
            logger.info("Exchange rate update completed successfully");
        } catch (Exception e) {
            logger.error("Failed to update exchange rates: {}", e.getMessage());
            // Don't rethrow - we want to continue even if API fails
        }
    }

    /**
     * Fetch rates from API and update database
     */
    private void fetchAndUpdateRates(String baseCurrency) {
        try {
            String url = String.format("%s%s?key=%s", apiUrl, baseCurrency, apiKey);
            
            // For demo purposes, using free tier without key
            if ("demo".equals(apiKey)) {
                url = String.format("%s%s", apiUrl, baseCurrency);
            }

            var response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(org.json.JSONObject.class)
                .block(); // Blocking call for simplicity in scheduler

            if (response != null && response.has("rates")) {
                org.json.JSONObject rates = response.getJSONObject("rates");
                
                // Update all currency pairs
                for (String targetCurrency : rates.keySet()) {
                    Double rate = rates.getDouble(targetCurrency);
                    
                    // Store both directions: BASE->TARGET and TARGET->BASE
                    currencyConversionService.updateExchangeRate(baseCurrency, targetCurrency, rate, "API");
                    
                    // Calculate and store reverse rate
                    if (rate > 0) {
                        Double reverseRate = 1.0 / rate;
                        currencyConversionService.updateExchangeRate(targetCurrency, baseCurrency, reverseRate, "API_DERIVED");
                    }
                }
                
                logger.info("Updated rates for base currency: {}", baseCurrency);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch rates for {}: {}", baseCurrency, e.getMessage());
        }
    }

    /**
     * Manual trigger for rate update (can be called from controller)
     */
    public void manualUpdate() {
        logger.info("Manual exchange rate update triggered");
        updateExchangeRates();
    }
}
