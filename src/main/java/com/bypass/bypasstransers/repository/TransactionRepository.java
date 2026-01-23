package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 *
 * @author Vickeller.01
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySyncStatus(String status);

    @Query("SELECT t.fromAccount, SUM(t.amount) FROM Transaction t GROUP BY t.fromAccount")
    Iterable<Object[]> volumeByAccount();

    @Query("SELECT function('date', t.date), SUM(t.amount) FROM Transaction t GROUP BY function('date', t.date) ORDER BY function('date', t.date) DESC")
    Iterable<Object[]> dailyVolume();

    @Query("SELECT t.fromAccount, SUM(t.fee) FROM Transaction t GROUP BY t.fromAccount")
    Iterable<Object[]> feesByAccount();
}