
package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for simple transaction analysis queries.
 */
@Repository
public interface TransactionAnalysisRepo extends JpaRepository<Transaction, Long> {

    @Query("SELECT t.fromAccount, SUM(t.amount) FROM Transaction t GROUP BY t.fromAccount")
    List<Object[]> volumeByAccount();

}