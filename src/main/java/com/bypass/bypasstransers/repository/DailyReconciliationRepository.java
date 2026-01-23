package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.DailyReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyReconciliationRepository extends JpaRepository<DailyReconciliation, Long> {

}
