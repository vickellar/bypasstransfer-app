package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.config.ExchangeRateScheduler;
import com.bypass.bypasstransers.model.ExchangeRate;
import com.bypass.bypasstransers.repository.ExchangeRateRepository;
import com.bypass.bypasstransers.service.CurrencyConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/exchange-rates")
public class ExchangeRateController {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Autowired
    private ExchangeRateScheduler scheduler;

    /**
     * Get all exchange rates
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllRates() {
        try {
            List<ExchangeRate> rates = exchangeRateRepository.findAll();
            return ResponseEntity.ok(rates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching rates: " + e.getMessage());
        }
    }

    /**
     * Get rate for specific currency pair
     */
    @GetMapping("/{from}/{to}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getRate(@PathVariable String from, @PathVariable String to) {
        try {
            BigDecimal rate = currencyConversionService.getExchangeRate(from, to);
            Map<String, Object> response = new HashMap<>();
            response.put("from", from);
            response.put("to", to);
            response.put("rate", rate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Rate not found: " + e.getMessage());
        }
    }

    /**
     * Trigger manual update of exchange rates from API
     */
    @PostMapping("/update")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateRates() {
        try {
            scheduler.manualUpdate();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Exchange rates updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating rates: " + e.getMessage());
        }
    }

    /**
     * Manually override a specific rate
     */
    @PutMapping("/{from}/{to}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateRate(
            @PathVariable String from,
            @PathVariable String to,
            @RequestBody Map<String, BigDecimal> rateData) {
        try {
            BigDecimal rate = rateData.get("rate");
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Invalid rate value");
            }

            currencyConversionService.updateExchangeRate(from, to, rate, "MANUAL");
            
            // Also update reverse rate
            BigDecimal reverseRate = BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP);
            currencyConversionService.updateExchangeRate(to, from, reverseRate, "MANUAL_DERIVED");

            Map<String, Object> response = new HashMap<>();
            response.put("from", from);
            response.put("to", to);
            response.put("rate", rate);
            response.put("reverseRate", reverseRate);
            response.put("message", "Rate updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating rate: " + e.getMessage());
        }
    }

    /**
     * Get last update timestamp
     */
    @GetMapping("/last-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getLastUpdate() {
        try {
            List<ExchangeRate> rates = exchangeRateRepository.findAll();
            LocalDateTime lastUpdate = rates.stream()
                .map(ExchangeRate::getLastUpdated)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("lastUpdate", lastUpdate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching last update: " + e.getMessage());
        }
    }
}
