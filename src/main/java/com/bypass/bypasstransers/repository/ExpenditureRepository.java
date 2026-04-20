package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Expenditure;
import com.bypass.bypasstransers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
    
    List<Expenditure> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);
    
    List<Expenditure> findByCategoryAndDateBetweenOrderByDateDesc(String category, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(e.amount) FROM Expenditure e WHERE e.date BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpenditureForPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category, SUM(e.amount) FROM Expenditure e WHERE e.date BETWEEN :startDate AND :endDate GROUP BY e.category")
    List<Object[]> getExpenditureByCategoryForPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    List<Expenditure> findByRecordedBy(User user);
    
    void deleteByRecordedBy(com.bypass.bypasstransers.model.User user);
}
