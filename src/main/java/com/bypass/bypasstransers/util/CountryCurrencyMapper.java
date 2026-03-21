package com.bypass.bypasstransers.util;

import com.bypass.bypasstransers.enums.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map countries to their default currencies
 */
public class CountryCurrencyMapper {

    private static final Map<String, Currency> countryCurrencyMap = new HashMap<>();

    static {
        // Initialize country to currency mappings
        countryCurrencyMap.put("South Africa", Currency.ZAR);
        countryCurrencyMap.put("Russia", Currency.RUB);
        countryCurrencyMap.put("Zimbabwe", Currency.USD);
        countryCurrencyMap.put("United Kingdom", Currency.GBP);
        countryCurrencyMap.put("Germany", Currency.EUR);
        countryCurrencyMap.put("France", Currency.EUR);
        countryCurrencyMap.put("Italy", Currency.EUR);
        countryCurrencyMap.put("Spain", Currency.EUR);
        countryCurrencyMap.put("Nigeria", Currency.NGN);
        countryCurrencyMap.put("Kenya", Currency.KES);
        countryCurrencyMap.put("United States", Currency.USD);
        countryCurrencyMap.put("USA", Currency.USD);
    }

    /**
     * Get the default currency for a country
     */
    public static Currency getCurrencyForCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return Currency.USD; // Default fallback
        }
        
        Currency currency = countryCurrencyMap.get(country.trim());
        return currency != null ? currency : Currency.USD;
    }

    /**
     * Add or override a country-currency mapping
     */
    public static void addMapping(String country, Currency currency) {
        countryCurrencyMap.put(country, currency);
    }

    /**
     * Check if a country has a mapped currency
     */
    public static boolean hasMapping(String country) {
        return countryCurrencyMap.containsKey(country);
    }
}
