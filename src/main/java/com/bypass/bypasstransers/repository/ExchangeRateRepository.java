
package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository
        extends JpaRepository<ExchangeRate, Long> {

    ExchangeRate findTopByBaseCurrencyAndTargetCurrencyOrderByFetchedAtDesc(
            String base, String target);
}

