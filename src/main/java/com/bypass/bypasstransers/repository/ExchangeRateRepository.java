package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
    void deleteByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
