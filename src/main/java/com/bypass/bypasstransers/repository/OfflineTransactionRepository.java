package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.OfflineTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OfflineTransactionRepository extends JpaRepository<OfflineTransaction, Long> {
    
    List<OfflineTransaction> findBySyncStatus(String syncStatus);
    
    List<OfflineTransaction> findByUserIdAndSyncStatus(Long userId, String syncStatus);
    
    List<OfflineTransaction> findByUsernameAndSyncStatus(String username, String syncStatus);
    
    @Query("SELECT COUNT(o) FROM OfflineTransaction o WHERE o.syncStatus = 'PENDING'")
    long countPendingTransactions();
    
    @Query("SELECT COUNT(o) FROM OfflineTransaction o WHERE o.syncStatus = 'SYNCED'")
    long countSyncedTransactions();
    
    @Query("SELECT COUNT(o) FROM OfflineTransaction o WHERE o.syncStatus = 'FAILED'")
    long countFailedTransactions();
    
    @Query("SELECT o FROM OfflineTransaction o WHERE o.syncStatus = 'PENDING' ORDER BY o.offlineRecordedAt ASC")
    List<OfflineTransaction> findPendingTransactionsOrdered();
    
    @Query("SELECT o FROM OfflineTransaction o WHERE o.username = :username AND o.syncStatus = 'PENDING' ORDER BY o.offlineRecordedAt ASC")
    List<OfflineTransaction> findPendingTransactionsByUsernameOrdered(@Param("username") String username);
    
    @Query("SELECT o FROM OfflineTransaction o WHERE o.userId = :userId ORDER BY o.offlineRecordedAt DESC")
    List<OfflineTransaction> findByUserIdOrderByOfflineRecordedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT o FROM OfflineTransaction o WHERE o.username = :username ORDER BY o.offlineRecordedAt DESC")
    List<OfflineTransaction> findByUsernameOrderByOfflineRecordedAtDesc(@Param("username") String username);
    
    List<OfflineTransaction> findTop10ByOrderByOfflineRecordedAtDesc();
}
